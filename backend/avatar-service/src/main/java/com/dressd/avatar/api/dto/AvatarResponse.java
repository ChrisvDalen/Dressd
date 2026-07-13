package com.dressd.avatar.api.dto;

import com.dressd.avatar.domain.Avatar;
import com.dressd.common.domain.BodyType;
import java.util.UUID;

public record AvatarResponse(
        UUID id,
        UUID ownerId,
        String name,
        BodyType bodyType,
        ProportionsDto proportions
) {
    public static AvatarResponse from(Avatar a) {
        return new AvatarResponse(
                a.getId(),
                a.getOwnerId(),
                a.getName(),
                a.getBodyType(),
                ProportionsDto.from(a.getProportions())
        );
    }
}
