package com.dressd.wardrobe.api.dto;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Persists a garment after the user has confirmed (or corrected) the
 * recognition suggestion (SPEC.md flow A, step 5-6).
 */
public record CreateGarmentRequest(
        @NotNull GarmentCategory category,
        @NotBlank String imageUrl,
        String colorTag,
        String pattern,
        Season season,
        AnchorPointsDto anchorPoints
) {
}
