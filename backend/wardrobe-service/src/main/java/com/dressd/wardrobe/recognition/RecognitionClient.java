package com.dressd.wardrobe.recognition;

import com.dressd.common.domain.GarmentCategory;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the Python/FastAPI garment-recognition sidecar. When the sidecar is
 * disabled or unreachable it falls back to a deterministic local stub so the
 * scan flow still works end-to-end in development and tests.
 */
@Component
public class RecognitionClient {

    private static final Logger log = LoggerFactory.getLogger(RecognitionClient.class);

    private final RestClient restClient;
    private final RecognitionProperties properties;

    public RecognitionClient(RecognitionProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
    }

    public RecognitionResult recognize(byte[] imageBytes, String filename, String contentType) {
        if (!properties.isEnabled()) {
            return stub(imageBytes);
        }
        try {
            MultipartBodyBuilder body = new MultipartBodyBuilder();
            body.part("file", new NamedByteArrayResource(imageBytes, filename))
                    .contentType(MediaType.parseMediaType(
                            contentType == null ? MediaType.IMAGE_JPEG_VALUE : contentType));

            RecognitionResult result = restClient.post()
                    .uri("/recognize")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(RecognitionResult.class);

            if (result == null || result.imageBase64() == null) {
                throw new RestClientException("Empty recognition response");
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Recognition sidecar unavailable ({}), using local stub", ex.getMessage());
            return stub(imageBytes);
        }
    }

    /**
     * Offline fallback: passes the original image through unchanged and derives
     * a deterministic category/colour so the flow never hard-fails.
     */
    private RecognitionResult stub(byte[] imageBytes) {
        GarmentCategory[] categories = GarmentCategory.values();
        int hash = Math.abs(java.util.Arrays.hashCode(imageBytes));
        GarmentCategory category = categories[hash % categories.length];
        String[] palette = {"#3a5f8a", "#8a3a3a", "#3a8a5f", "#8a7a3a", "#5f3a8a"};
        String color = palette[hash % palette.length];
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return new RecognitionResult(category, color, "solid", base64);
    }
}
