package com.dressd.avatar;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.avatar.api.dto.AvatarResponse;
import com.dressd.avatar.api.dto.SaveAvatarRequest;
import com.dressd.common.domain.BodyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AvatarFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void bodyTypeCatalogHasAtLeastThreePresets() {
        ResponseEntity<Object[]> resp = rest.getForEntity("/api/avatars/body-types", Object[].class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void createAppliesBodyTypeDefaultProportions() {
        SaveAvatarRequest req = new SaveAvatarRequest("Me", BodyType.CURVY, null);
        ResponseEntity<AvatarResponse> created = rest.postForEntity("/api/avatars", req, AvatarResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AvatarResponse body = created.getBody();
        assertThat(body).isNotNull();
        assertThat(body.bodyType()).isEqualTo(BodyType.CURVY);
        // CURVY preset widens the hips beyond 1.0
        assertThat(body.proportions().hipWidth()).isGreaterThan(1.0f);
    }
}
