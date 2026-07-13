package com.dressd.wardrobe.api.dto;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;

/**
 * Partial metadata update (SPEC.md flow C, step 2). Null fields are left
 * unchanged.
 */
public record UpdateGarmentRequest(
        GarmentCategory category,
        String colorTag,
        String pattern,
        Season season,
        AnchorPointsDto anchorPoints
) {
}
