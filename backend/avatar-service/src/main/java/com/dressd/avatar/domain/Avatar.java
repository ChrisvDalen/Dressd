package com.dressd.avatar.domain;

import com.dressd.common.domain.BodyType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * A user's avatar/silhouette (SPEC.md section 4). No ML — a fixed body type
 * plus adjustable proportions; the client renders the silhouette.
 */
@Entity
@Table(name = "avatars", indexes = @Index(name = "idx_avatar_owner", columnList = "ownerId"))
public class Avatar {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private BodyType bodyType = BodyType.AVERAGE;

    @Embedded
    private Proportions proportions = new Proportions();

    public Avatar() {
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public Proportions getProportions() {
        return proportions;
    }

    public void setProportions(Proportions proportions) {
        this.proportions = proportions;
    }
}
