package com.dressd.wardrobe;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.common.auth.OwnerResolver;
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

/**
 * Verifies that {@code TOKEN} mode — what the {@code prod} profile defaults to —
 * actually refuses unauthenticated traffic, including traffic that presents the
 * {@code X-Owner-Id} header the DEV mode trusts.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dressd.auth.mode=TOKEN",
                "dressd.auth.secret=integration-test-secret-32-chars-min"
        })
@AutoConfigureTestRestTemplate
class TokenAuthIntegrationTest {

    private static final UUID OWNER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private OwnerResolver resolver;

    @Test
    void rejectsRequestsWithNoCredential() {
        ResponseEntity<String> response = rest.getForEntity("/api/garments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    @Test
    void ignoresTheOwnerHeaderSoItCannotBeUsedAsABypass() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Owner-Id", OWNER.toString());

        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsAForgedToken() {
        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.GET,
                new HttpEntity<>(bearer("v1.YWJj.ZGVm")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void acceptsAProperlySignedToken() {
        String token = resolver.tokenService().mint(OWNER);

        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void healthProbeStaysPublic() {
        assertThat(rest.getForEntity("/actuator/health", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
