package com.dressd.wardrobe.domain;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A single scanned wardrobe item (SPEC.md section 4).
 */
@Entity
@Table(name = "garments", indexes = {
        @Index(name = "idx_garment_owner", columnList = "ownerId"),
        @Index(name = "idx_garment_owner_category", columnList = "ownerId,category")
})
public class Garment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GarmentCategory category;

    /** URL/key of the cut-out PNG (transparent background) in object storage. */
    @Column(nullable = false, length = 1024)
    private String imageUrl;

    /** Dominant colour, stored as a hex string e.g. {@code #3355ff}. */
    @Column(length = 32)
    private String colorTag;

    @Column(length = 32)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Season season;

    @Embedded
    private AnchorPoints anchorPoints = new AnchorPoints();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Garment() {
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public GarmentCategory getCategory() {
        return category;
    }

    public void setCategory(GarmentCategory category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getColorTag() {
        return colorTag;
    }

    public void setColorTag(String colorTag) {
        this.colorTag = colorTag;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public AnchorPoints getAnchorPoints() {
        return anchorPoints;
    }

    public void setAnchorPoints(AnchorPoints anchorPoints) {
        this.anchorPoints = anchorPoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
