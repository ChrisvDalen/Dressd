package com.dressd.outfit.domain;

import com.dressd.common.domain.GarmentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.UUID;

/**
 * One garment placed on the avatar within an outfit (SPEC.md section 4). Stores
 * only positioning — the client renders the actual image (no server-side
 * rendering).
 */
@Embeddable
public class GarmentLayer {

    @Column(nullable = false)
    private UUID garmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GarmentCategory category;

    @Column(nullable = false)
    private int zIndex;

    private float offsetX;
    private float offsetY;
    private float scale = 1.0f;

    public GarmentLayer() {
    }

    public GarmentLayer(UUID garmentId, GarmentCategory category, int zIndex,
                        float offsetX, float offsetY, float scale) {
        this.garmentId = garmentId;
        this.category = category;
        this.zIndex = zIndex;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scale = scale;
    }

    public UUID getGarmentId() {
        return garmentId;
    }

    public void setGarmentId(UUID garmentId) {
        this.garmentId = garmentId;
    }

    public GarmentCategory getCategory() {
        return category;
    }

    public void setCategory(GarmentCategory category) {
        this.category = category;
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}
