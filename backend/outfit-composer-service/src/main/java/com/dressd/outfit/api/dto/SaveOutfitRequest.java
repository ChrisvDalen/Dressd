package com.dressd.outfit.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SaveOutfitRequest(
        @NotNull UUID avatarId,
        String name,
        @Valid List<GarmentLayerDto> garmentLayers
) {
}
