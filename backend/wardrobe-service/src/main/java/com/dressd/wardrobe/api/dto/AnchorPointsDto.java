package com.dressd.wardrobe.api.dto;

import com.dressd.wardrobe.domain.AnchorPoints;

public record AnchorPointsDto(float shoulderY, float waistY, float hemY, float widthScale) {

    public static AnchorPointsDto from(AnchorPoints a) {
        return new AnchorPointsDto(a.getShoulderY(), a.getWaistY(), a.getHemY(), a.getWidthScale());
    }

    public AnchorPoints toEntity() {
        return new AnchorPoints(shoulderY, waistY, hemY, widthScale);
    }
}
