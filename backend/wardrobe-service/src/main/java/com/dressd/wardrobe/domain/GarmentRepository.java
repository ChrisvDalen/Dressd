package com.dressd.wardrobe.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GarmentRepository
        extends JpaRepository<Garment, UUID>, JpaSpecificationExecutor<Garment> {

    Optional<Garment> findByIdAndOwnerId(UUID id, UUID ownerId);
}
