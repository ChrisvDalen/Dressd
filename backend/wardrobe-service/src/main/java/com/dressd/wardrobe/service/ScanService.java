package com.dressd.wardrobe.service;

import com.dressd.wardrobe.api.dto.ScanResultResponse;
import com.dressd.wardrobe.recognition.RecognitionClient;
import com.dressd.wardrobe.recognition.RecognitionResult;
import com.dressd.wardrobe.storage.ObjectStorage;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the scan flow (SPEC.md flow A): send the photo to recognition,
 * store the returned cut-out PNG, and hand back a suggestion for the user to
 * confirm. No {@code Garment} is persisted until the user confirms.
 */
@Service
public class ScanService {

    private final RecognitionClient recognitionClient;
    private final ObjectStorage storage;

    public ScanService(RecognitionClient recognitionClient, ObjectStorage storage) {
        this.recognitionClient = recognitionClient;
        this.storage = storage;
    }

    public ScanResultResponse scan(UUID ownerId, byte[] imageBytes, String filename, String contentType) {
        RecognitionResult result = recognitionClient.recognize(imageBytes, filename, contentType);

        byte[] cutout = Base64.getDecoder().decode(result.imageBase64());
        String key = "garments/%s/%s.png".formatted(ownerId, UUID.randomUUID());
        String imageUrl = storage.store(key, cutout, "image/png");

        return new ScanResultResponse(
                imageUrl,
                result.category(),
                result.colorTag(),
                result.pattern()
        );
    }
}
