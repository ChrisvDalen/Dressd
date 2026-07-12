package com.dressd.avatar.api.dto;

import com.dressd.common.domain.BodyType;
import jakarta.validation.constraints.NotNull;

/**
 * Create or update an avatar. When {@code proportions} is null the body type's
 * default preset is applied.
 */
public record SaveAvatarRequest(
        String name,
        @NotNull BodyType bodyType,
        ProportionsDto proportions
) {
}
