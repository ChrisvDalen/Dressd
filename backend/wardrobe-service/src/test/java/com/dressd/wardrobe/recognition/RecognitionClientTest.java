package com.dressd.wardrobe.recognition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dressd.common.domain.GarmentCategory;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Exercises the real RestClient multipart request path to the recognition
 * sidecar — the path a stubbed (enabled=false) test would skip. This is the
 * path that surfaced the missing reactive-streams dependency in live e2e.
 */
class RecognitionClientTest {

    @Test
    void parsesRecognitionResponseOverHttp() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String png = Base64.getEncoder().encodeToString("png-bytes".getBytes());
        server.expect(requestTo("http://reco.test/recognize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"category\":\"TOP\",\"colorTag\":\"#112233\",\"pattern\":\"solid\",\"imageBase64\":\""
                                + png + "\"}",
                        MediaType.APPLICATION_JSON));

        RecognitionProperties props = new RecognitionProperties();
        props.setBaseUrl("http://reco.test");
        props.setEnabled(true);

        RecognitionClient client = new RecognitionClient(props, builder);
        RecognitionResult result = client.recognize("hello".getBytes(), "shirt.jpg", "image/jpeg");

        assertThat(result.category()).isEqualTo(GarmentCategory.TOP);
        assertThat(result.colorTag()).isEqualTo("#112233");
        assertThat(result.imageBase64()).isEqualTo(png);
        server.verify();
    }

    @Test
    void fallsBackToStubWhenSidecarErrors() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://reco.test/recognize")).andRespond(withServerError());

        RecognitionProperties props = new RecognitionProperties();
        props.setBaseUrl("http://reco.test");
        props.setEnabled(true);

        RecognitionClient client = new RecognitionClient(props, builder);
        RecognitionResult result = client.recognize("hello".getBytes(), "shirt.jpg", "image/jpeg");

        // Stub fallback still yields a usable result (category + passthrough image).
        assertThat(result.category()).isNotNull();
        assertThat(result.imageBase64()).isEqualTo(Base64.getEncoder().encodeToString("hello".getBytes()));
    }
}
