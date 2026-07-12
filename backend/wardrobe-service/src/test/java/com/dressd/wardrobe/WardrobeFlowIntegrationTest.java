package com.dressd.wardrobe;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.wardrobe.api.dto.CreateGarmentRequest;
import com.dressd.wardrobe.api.dto.GarmentResponse;
import com.dressd.wardrobe.api.dto.ScanResultResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WardrobeFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void scanThenConfirmThenFilter() {
        // 1. Scan a photo -> recognition stub returns a suggestion + stored image.
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource("fake-jpeg-bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "shirt.jpg";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<ScanResultResponse> scanResp = rest.postForEntity(
                "/api/scan", new HttpEntity<>(form, headers), ScanResultResponse.class);

        assertThat(scanResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ScanResultResponse scan = scanResp.getBody();
        assertThat(scan).isNotNull();
        assertThat(scan.imageUrl()).contains("/media/garments/");
        assertThat(scan.suggestedCategory()).isNotNull();

        // 2. Confirm/correct -> persist a TOP garment.
        CreateGarmentRequest create = new CreateGarmentRequest(
                GarmentCategory.TOP, scan.imageUrl(), "#3355ff", "solid", null, null);
        ResponseEntity<GarmentResponse> created = rest.postForEntity(
                "/api/garments", create, GarmentResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().category()).isEqualTo(GarmentCategory.TOP);

        // 3. Persist a BOTTOM garment too.
        rest.postForEntity("/api/garments",
                new CreateGarmentRequest(GarmentCategory.BOTTOM, scan.imageUrl(), "#222222", null, null, null),
                GarmentResponse.class);

        // 4. Filter by category TOP -> exactly one.
        ResponseEntity<GarmentResponse[]> tops = rest.getForEntity(
                "/api/garments?category=TOP", GarmentResponse[].class);
        assertThat(tops.getBody()).hasSize(1);
        assertThat(tops.getBody()[0].category()).isEqualTo(GarmentCategory.TOP);

        // 5. Filter by colour substring.
        ResponseEntity<GarmentResponse[]> blue = rest.getForEntity(
                "/api/garments?color=3355", GarmentResponse[].class);
        assertThat(blue.getBody()).hasSize(1);
    }

    @Test
    void deleteRemovesGarment() {
        CreateGarmentRequest create = new CreateGarmentRequest(
                GarmentCategory.SHOES, "/media/x.png", "#000000", null, null, null);
        GarmentResponse created = rest.postForObject("/api/garments", create, GarmentResponse.class);

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/garments/" + created.id(), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refetch = rest.getForEntity("/api/garments/" + created.id(), String.class);
        assertThat(refetch.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
