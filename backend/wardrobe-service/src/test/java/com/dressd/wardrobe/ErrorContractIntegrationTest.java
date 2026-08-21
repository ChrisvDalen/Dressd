package com.dressd.wardrobe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The shared handler ends in a catch-all, which must not swallow the status of
 * Spring's own MVC exceptions and report everything as a 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ErrorContractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void wrongMethodIsMethodNotAllowed() {
        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.PUT,
                new HttpEntity<>("{}", jsonHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void unsupportedBodyTypeIsUnsupportedMediaType() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.POST,
                new HttpEntity<>("not json", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void unparseableBodyIsBadRequest() {
        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.POST,
                new HttpEntity<>("{ not json", jsonHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownEnumValueIsBadRequest() {
        ResponseEntity<String> response = rest.getForEntity("/api/garments?category=HAT", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void malformedPathVariableIsBadRequest() {
        ResponseEntity<String> response = rest.getForEntity("/api/garments/not-a-uuid", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validationFailureListsTheOffendingFields() {
        // category and imageUrl are both required.
        ResponseEntity<String> response = rest.exchange("/api/garments", HttpMethod.POST,
                new HttpEntity<>("{}", jsonHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Validation failed").contains("category");
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
