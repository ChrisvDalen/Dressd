package com.dressd.avatar;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.avatar.api.dto.AvatarResponse;
import com.dressd.avatar.api.dto.ProportionsDto;
import com.dressd.avatar.api.dto.SaveAvatarRequest;
import com.dressd.common.domain.BodyType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
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

    @Test
    void createHonoursExplicitProportionsInsteadOfThePreset() {
        SaveAvatarRequest req = new SaveAvatarRequest(
                "Custom", BodyType.SLIM, new ProportionsDto(1.10f, 0.80f, 0.95f));
        ResponseEntity<AvatarResponse> created = rest.postForEntity("/api/avatars", req, AvatarResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // The caller's numbers win over the SLIM preset, proving proportions are not
        // silently overwritten by the catalog.
        assertThat(created.getBody().proportions().height()).isEqualTo(1.10f);
        assertThat(created.getBody().proportions().shoulderWidth()).isEqualTo(0.80f);
        assertThat(created.getBody().proportions().hipWidth()).isEqualTo(0.95f);
    }

    @Test
    void updateReplacesNameBodyTypeAndProportions() {
        UUID id = create("Old", BodyType.AVERAGE);

        SaveAvatarRequest req = new SaveAvatarRequest("New", BodyType.CURVY,
                new ProportionsDto(0.95f, 1.05f, 1.20f));
        ResponseEntity<AvatarResponse> updated = rest.exchange(
                "/api/avatars/" + id, HttpMethod.PUT,
                new HttpEntity<>(req), AvatarResponse.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("New");
        assertThat(updated.getBody().bodyType()).isEqualTo(BodyType.CURVY);
        assertThat(updated.getBody().proportions().hipWidth()).isEqualTo(1.20f);
    }

    @Test
    void deleteRemovesTheAvatar() {
        UUID id = create("Gone", BodyType.AVERAGE);

        ResponseEntity<Void> deleted = rest.exchange("/api/avatars/" + id,
                HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(rest.getForEntity("/api/avatars/" + id, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anotherOwnerCannotReadUpdateOrDeleteMyAvatar() {
        UUID mine = create("Private", BodyType.AVERAGE);
        HttpHeaders other = new HttpHeaders();
        other.set("X-Owner-Id", "99999999-9999-9999-9999-999999999999");

        // Owner-scoped lookups read a foreign id as 404, so its existence is not
        // disclosed and the resource is unreachable.
        assertThat(rest.exchange("/api/avatars/" + mine, HttpMethod.GET,
                new HttpEntity<>(other), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.exchange("/api/avatars/" + mine, HttpMethod.PUT,
                new HttpEntity<>(new SaveAvatarRequest("stolen", BodyType.CURVY, null), other),
                String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.exchange("/api/avatars/" + mine, HttpMethod.DELETE,
                new HttpEntity<>(other), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // The original is untouched.
        assertThat(rest.getForEntity("/api/avatars/" + mine, AvatarResponse.class).getBody().name())
                .isEqualTo("Private");
    }

    @Test
    void listReturnsOnlyTheRequestingOwnersAvatars() {
        create("Mine", BodyType.AVERAGE);

        HttpHeaders other = new HttpHeaders();
        other.set("X-Owner-Id", "88888888-8888-8888-8888-888888888888");
        // The other owner starts with nothing of their own.
        assertThat(rest.exchange("/api/avatars", HttpMethod.GET,
                new HttpEntity<>(other), AvatarResponse[].class).getBody())
                .isEmpty();
    }

    private UUID create(String name, BodyType bodyType) {
        ResponseEntity<AvatarResponse> created = rest.postForEntity(
                "/api/avatars", new SaveAvatarRequest(name, bodyType, null), AvatarResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().id();
    }
}
