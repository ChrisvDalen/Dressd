package com.dressd.wardrobe.service;

import com.dressd.wardrobe.api.dto.ScanResultResponse;
import com.dressd.wardrobe.recognition.RecognitionClient;
import com.dressd.wardrobe.recognition.RecognitionResult;
import com.dressd.wardrobe.storage.GarmentImages;
import com.dressd.wardrobe.storage.ObjectStorage;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the scan flow (SPEC.md flow A): send the photo to recognition,
 * store the returned cut-out PNG, and hand back a suggestion for the user to
 * confirm. No {@code Garment} is persisted until the user confirms, so the cut-out
 * lands in the pending area and is promoted (or swept up) later.
 */
@Service
public class ScanService {

    private final RecognitionClient recognitionClient;
    private final ObjectStorage storage;
    private final GarmentImages images;

    public ScanService(RecognitionClient recognitionClient, ObjectStorage storage, GarmentImages images) {
        this.recognitionClient = recognitionClient;
        this.storage = storage;
        this.images = images;
    }

    public ScanResultResponse scan(UUID ownerId, byte[] imageBytes, String filename, String contentType) {
        RecognitionResult result = recognitionClient.recognize(imageBytes, filename, contentType);

        byte[] cutout = Base64.getDecoder().decode(result.imageBase64());
        String imageUrl = storage.store(images.pendingKey(ownerId), cutout, "image/png");

        return new ScanResultResponse(
                imageUrl,
                result.category(),
                result.colorTag(),
                result.pattern()
        );
    }
}
