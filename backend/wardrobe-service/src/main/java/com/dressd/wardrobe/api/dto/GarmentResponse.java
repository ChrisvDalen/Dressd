package com.dressd.wardrobe.api.dto;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import com.dressd.wardrobe.domain.Garment;
import java.time.Instant;
import java.util.UUID;

public record GarmentResponse(
        UUID id,
        UUID ownerId,
        GarmentCategory category,
        String imageUrl,
        String colorTag,
        String pattern,
        Season season,
        AnchorPointsDto anchorPoints,
        Instant createdAt,
        Instant updatedAt
) {
    public static GarmentResponse from(Garment g) {
        return new GarmentResponse(
                g.getId(),
                g.getOwnerId(),
                g.getCategory(),
                g.getImageUrl(),
                g.getColorTag(),
                g.getPattern(),
                g.getSeason(),
                AnchorPointsDto.from(g.getAnchorPoints()),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}
