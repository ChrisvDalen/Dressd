package com.dressd.outfit.api.dto;

import com.dressd.outfit.domain.Outfit;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OutfitResponse(
        UUID id,
        UUID ownerId,
        UUID avatarId,
        String name,
        List<GarmentLayerDto> garmentLayers,
        Instant createdAt,
        Instant updatedAt
) {
    public static OutfitResponse from(Outfit o) {
        return new OutfitResponse(
                o.getId(),
                o.getOwnerId(),
                o.getAvatarId(),
                o.getName(),
                o.getGarmentLayers().stream().map(GarmentLayerDto::from).toList(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
