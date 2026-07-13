package com.dressd.wardrobe.domain;

import jakarta.persistence.Embeddable;

/**
 * Normalised (0..1) vertical anchor points describing where a garment attaches
 * on the silhouette, plus a horizontal width scale. The Angular canvas uses
 * these to position each layer (SPEC.md section 4).
 */
@Embeddable
public class AnchorPoints {

    private float shoulderY = 0.20f;
    private float waistY = 0.45f;
    private float hemY = 0.70f;
    private float widthScale = 1.0f;

    public AnchorPoints() {
    }

    public AnchorPoints(float shoulderY, float waistY, float hemY, float widthScale) {
        this.shoulderY = shoulderY;
        this.waistY = waistY;
        this.hemY = hemY;
        this.widthScale = widthScale;
    }

    public float getShoulderY() {
        return shoulderY;
    }

    public void setShoulderY(float shoulderY) {
        this.shoulderY = shoulderY;
    }

    public float getWaistY() {
        return waistY;
    }

    public void setWaistY(float waistY) {
        this.waistY = waistY;
    }

    public float getHemY() {
        return hemY;
    }

    public void setHemY(float hemY) {
        this.hemY = hemY;
    }

    public float getWidthScale() {
        return widthScale;
    }

    public void setWidthScale(float widthScale) {
        this.widthScale = widthScale;
    }
}
