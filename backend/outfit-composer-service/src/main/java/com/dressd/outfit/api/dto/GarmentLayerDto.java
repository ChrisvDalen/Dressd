package com.dressd.outfit.api.dto;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.outfit.domain.GarmentLayer;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GarmentLayerDto(
        @NotNull UUID garmentId,
        @NotNull GarmentCategory category,
        int zIndex,
        @DecimalMin("-2.0") @DecimalMax("2.0") float offsetX,
        @DecimalMin("-2.0") @DecimalMax("2.0") float offsetY,
        @DecimalMin("0.1") @DecimalMax("5.0") float scale
) {
    public static GarmentLayerDto from(GarmentLayer l) {
        return new GarmentLayerDto(l.getGarmentId(), l.getCategory(), l.getZIndex(),
                l.getOffsetX(), l.getOffsetY(), l.getScale());
    }

    /** Converts to an entity at the given stacking position. */
    public GarmentLayer toEntity(int resolvedZIndex) {
        float appliedScale = scale <= 0 ? 1.0f : scale;
        return new GarmentLayer(garmentId, category, resolvedZIndex, offsetX, offsetY, appliedScale);
    }
}
