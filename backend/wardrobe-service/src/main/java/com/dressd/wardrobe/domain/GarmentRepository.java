package com.dressd.wardrobe.domain;

import com.dressd.common.domain.GarmentCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GarmentRepository
        extends JpaRepository<Garment, UUID>, JpaSpecificationExecutor<Garment> {

    Optional<Garment> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Garment> findByOwnerIdAndCategoryOrderByUpdatedAtDesc(UUID ownerId, GarmentCategory category);

    long deleteByIdAndOwnerId(UUID id, UUID ownerId);
}
