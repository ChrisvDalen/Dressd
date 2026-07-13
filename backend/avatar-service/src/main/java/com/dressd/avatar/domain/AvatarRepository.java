package com.dressd.avatar.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarRepository extends JpaRepository<Avatar, UUID> {

    List<Avatar> findByOwnerId(UUID ownerId);

    Optional<Avatar> findByIdAndOwnerId(UUID id, UUID ownerId);
}
