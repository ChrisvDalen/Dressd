package com.dressd.outfit.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A saved outfit: which garment layers, in which order, at which offset/scale
 * (SPEC.md section 4). Positions only — no rendered image.
 */
@Entity
@Table(name = "outfits", indexes = @Index(name = "idx_outfit_owner", columnList = "ownerId"))
public class Outfit {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private UUID avatarId;

    @Column(length = 120)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "outfit_layers", joinColumns = @JoinColumn(name = "outfit_id"))
    @OrderBy("zIndex ASC")
    private List<GarmentLayer> garmentLayers = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Outfit() {
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

    public UUID getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(UUID avatarId) {
        this.avatarId = avatarId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GarmentLayer> getGarmentLayers() {
        return garmentLayers;
    }

    public void setGarmentLayers(List<GarmentLayer> garmentLayers) {
        this.garmentLayers = garmentLayers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
