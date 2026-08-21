package com.dressd.wardrobe;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.wardrobe.api.dto.CreateGarmentRequest;
import com.dressd.wardrobe.api.dto.GarmentResponse;
import com.dressd.wardrobe.api.dto.ScanResultResponse;
import com.dressd.wardrobe.storage.StorageProperties;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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
@AutoConfigureTestRestTemplate
class WardrobeFlowIntegrationTest {

    /** Mirrors {@code PageResponse<GarmentResponse>} for deserialisation. */
    record GarmentPage(List<GarmentResponse> content, int page, int size,
                       long totalElements, int totalPages, boolean first, boolean last) {
    }

    private static final ParameterizedTypeReference<GarmentPage> GARMENT_PAGE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void scanThenConfirmThenFilter() {
        // 1. Scan a photo -> recognition stub returns a suggestion + a pending image.
        ScanResultResponse scan = scan();
        assertThat(scan.imageUrl()).contains("/media/pending/");
        assertThat(scan.suggestedCategory()).isNotNull();

        // 2. Confirm/correct -> persist a TOP garment. Confirming promotes the image
        //    out of the pending area.
        CreateGarmentRequest create = new CreateGarmentRequest(
                GarmentCategory.TOP, scan.imageUrl(), "#3355ff", "solid", null, null);
        ResponseEntity<GarmentResponse> created = rest.postForEntity(
                "/api/garments", create, GarmentResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().category()).isEqualTo(GarmentCategory.TOP);
        assertThat(created.getBody().imageUrl()).contains("/media/garments/");

        // 3. Persist a BOTTOM garment from its own scan.
        rest.postForEntity("/api/garments",
                new CreateGarmentRequest(GarmentCategory.BOTTOM, scan().imageUrl(), "#222222",
                        null, null, null),
                GarmentResponse.class);

        // 4. Filter by category TOP -> exactly one.
        GarmentPage tops = page("/api/garments?category=TOP");
        assertThat(tops.content()).hasSize(1);
        assertThat(tops.content().getFirst().category()).isEqualTo(GarmentCategory.TOP);
        assertThat(tops.totalElements()).isEqualTo(1);

        // 5. Filter by colour substring.
        assertThat(page("/api/garments?color=3355").content()).hasSize(1);
    }

    @Test
    void listIsPagedAndCapsPageSize() {
        for (int i = 0; i < 3; i++) {
            rest.postForEntity("/api/garments",
                    new CreateGarmentRequest(GarmentCategory.ACCESSORY, scan().imageUrl(), "#0f0f0f",
                            null, null, null),
                    GarmentResponse.class);
        }

        GarmentPage firstPage = page("/api/garments?category=ACCESSORY&page=0&size=2");
        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();

        GarmentPage secondPage = page("/api/garments?category=ACCESSORY&page=1&size=2");
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.last()).isTrue();

        // An absurd page size is clamped rather than honoured.
        assertThat(page("/api/garments?size=100000").size()).isEqualTo(200);
    }

    @Test
    void deleteRemovesGarmentAndItsStoredImage() {
        GarmentResponse created = rest.postForObject("/api/garments",
                new CreateGarmentRequest(GarmentCategory.SHOES, scan().imageUrl(), "#000000",
                        null, null, null),
                GarmentResponse.class);

        Path storedImage = mediaPath(created.imageUrl());
        assertThat(storedImage).exists();

        ResponseEntity<Void> deleted = rest.exchange(
                "/api/garments/" + created.id(), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refetch = rest.getForEntity("/api/garments/" + created.id(), String.class);
        assertThat(refetch.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(storedImage).doesNotExist();
    }

    @Test
    void rejectsAnImageUrlThatIsNotOneOfOurScans() {
        ResponseEntity<String> response = rest.postForEntity("/api/garments",
                new CreateGarmentRequest(GarmentCategory.TOP, "https://evil.example/tracker.png",
                        null, null, null, null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("not a Dressd media URL");
    }

    @Test
    void rejectsAnotherOwnersPendingImage() {
        String foreignScan = scan("11111111-1111-1111-1111-111111111111").imageUrl();

        // The default (demo) owner tries to claim it.
        ResponseEntity<String> response = rest.postForEntity("/api/garments",
                new CreateGarmentRequest(GarmentCategory.TOP, foreignScan, null, null, null, null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("does not reference one of your scans");
    }

    @Test
    void rejectsAMalformedOwnerHeaderWithBadRequestNotServerError() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Owner-Id", "definitely-not-a-uuid");

        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("not a UUID");
    }

    @Test
    void rejectsANonImageUpload() {
        ResponseEntity<String> response = rest.postForEntity("/api/scan",
                multipart("#!/bin/sh".getBytes(), "payload.sh", MediaType.TEXT_PLAIN, null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Unsupported image type");
    }

    @Test
    void actuatorHealthIsReachableWithoutACredential() {
        ResponseEntity<String> health = rest.getForEntity("/actuator/health", String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("UP");
    }

    private GarmentPage page(String uri) {
        ResponseEntity<String> response = rest.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY, String.class);
        assertThat(response.getStatusCode())
                .withFailMessage("GET %s returned %s: %s", uri, response.getStatusCode(), response.getBody())
                .isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructType(GARMENT_PAGE.getType()));
        } catch (Exception exception) {
            throw new AssertionError("Could not deserialize garment page: " + response.getBody(), exception);
        }
    }

    private ScanResultResponse scan() {
        return scan(null);
    }

    private ScanResultResponse scan(String ownerId) {
        ResponseEntity<ScanResultResponse> response = rest.postForEntity("/api/scan",
                multipart("fake-jpeg-bytes".getBytes(), "shirt.jpg", MediaType.IMAGE_JPEG, ownerId),
                ScanResultResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static HttpEntity<MultiValueMap<String, Object>> multipart(
            byte[] bytes, String filename, MediaType partType, String ownerId) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(partType);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new HttpEntity<>(new NamedResource(bytes, filename), partHeaders));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (ownerId != null) {
            headers.set("X-Owner-Id", ownerId);
        }
        return new HttpEntity<>(form, headers);
    }

    /** Maps a {@code /media/...} URL back to its file on disk. */
    private Path mediaPath(String imageUrl) {
        String key = imageUrl.substring((storageProperties.getLocal().getPublicBaseUrl() + "/").length());
        return Path.of(storageProperties.getLocal().getRoot()).resolve(key);
    }

    private static final class NamedResource extends ByteArrayResource {
        private final String filename;

        NamedResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
