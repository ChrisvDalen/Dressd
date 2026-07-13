package com.dressd.wardrobe.recognition;

import com.dressd.common.domain.GarmentCategory;

/**
 * Response from the recognition sidecar: the background-removed PNG (base64)
 * plus the suggested metadata the user can confirm or correct.
 */
public record RecognitionResult(
        GarmentCategory category,
        String colorTag,
        String pattern,
        String imageBase64
) {
}
