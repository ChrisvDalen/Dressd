package com.dressd.wardrobe.api.dto;

import com.dressd.common.domain.GarmentCategory;

/**
 * Returned right after a photo is scanned (SPEC.md flow A, step 3-4): the
 * stored cut-out image plus the suggested metadata the user can confirm or
 * correct before saving. Nothing is persisted yet at this point.
 */
public record ScanResultResponse(
        String imageUrl,
        GarmentCategory suggestedCategory,
        String suggestedColorTag,
        String suggestedPattern
) {
}
