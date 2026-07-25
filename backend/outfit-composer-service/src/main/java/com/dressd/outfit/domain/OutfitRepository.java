package com.dressd.outfit.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitRepository extends JpaRepository<Outfit, UUID> {

    Page<Outfit> findByOwnerId(UUID ownerId, Pageable pageable);

    Optional<Outfit> findByIdAndOwnerId(UUID id, UUID ownerId);
}
