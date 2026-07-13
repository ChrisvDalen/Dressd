package com.dressd.outfit.api.dto;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.outfit.domain.GarmentLayer;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GarmentLayerDto(
        @NotNull UUID garmentId,
        @NotNull GarmentCategory category,
        int zIndex,
        float offsetX,
        float offsetY,
        float scale
) {
    public static GarmentLayerDto from(GarmentLayer l) {
        return new GarmentLayerDto(l.getGarmentId(), l.getCategory(), l.getZIndex(),
                l.getOffsetX(), l.getOffsetY(), l.getScale());
    }

    public GarmentLayer toEntity() {
        float appliedScale = scale <= 0 ? 1.0f : scale;
        return new GarmentLayer(garmentId, category, zIndex, offsetX, offsetY, appliedScale);
    }
}
