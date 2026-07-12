package com.dressd.avatar.api.dto;

import com.dressd.avatar.domain.Proportions;

public record ProportionsDto(float height, float shoulderWidth, float hipWidth) {

    public static ProportionsDto from(Proportions p) {
        return new ProportionsDto(p.getHeight(), p.getShoulderWidth(), p.getHipWidth());
    }

    public Proportions toEntity() {
        return new Proportions(height, shoulderWidth, hipWidth);
    }
}
