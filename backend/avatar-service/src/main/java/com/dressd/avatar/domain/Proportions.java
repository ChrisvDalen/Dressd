package com.dressd.avatar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Adjustable silhouette proportions (SPEC.md section 4). Values are relative
 * multipliers around 1.0 that the client canvas applies to the base silhouette.
 */
@Embeddable
public class Proportions {

    @Column(name = "height")
    private float height = 1.0f;

    @Column(name = "shoulder_width")
    private float shoulderWidth = 1.0f;

    @Column(name = "hip_width")
    private float hipWidth = 1.0f;

    public Proportions() {
    }

    public Proportions(float height, float shoulderWidth, float hipWidth) {
        this.height = height;
        this.shoulderWidth = shoulderWidth;
        this.hipWidth = hipWidth;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getShoulderWidth() {
        return shoulderWidth;
    }

    public void setShoulderWidth(float shoulderWidth) {
        this.shoulderWidth = shoulderWidth;
    }

    public float getHipWidth() {
        return hipWidth;
    }

    public void setHipWidth(float hipWidth) {
        this.hipWidth = hipWidth;
    }
}
