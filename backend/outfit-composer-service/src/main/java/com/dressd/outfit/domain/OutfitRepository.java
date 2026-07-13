package com.dressd.outfit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitRepository extends JpaRepository<Outfit, UUID> {

    List<Outfit> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    Optional<Outfit> findByIdAndOwnerId(UUID id, UUID ownerId);
}
