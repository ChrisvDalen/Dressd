package com.dressd.avatar.service;

import com.dressd.avatar.api.dto.SaveAvatarRequest;
import com.dressd.avatar.domain.Avatar;
import com.dressd.avatar.domain.AvatarRepository;
import com.dressd.avatar.domain.BodyTypeCatalog;
import com.dressd.common.web.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AvatarService {

    private final AvatarRepository repository;

    public AvatarService(AvatarRepository repository) {
        this.repository = repository;
    }

    public Avatar create(UUID ownerId, SaveAvatarRequest req) {
        Avatar a = new Avatar();
        a.setOwnerId(ownerId);
        apply(a, req);
        return repository.save(a);
    }

    @Transactional(readOnly = true)
    public List<Avatar> list(UUID ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Avatar get(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Avatar not found: " + id));
    }

    public Avatar update(UUID ownerId, UUID id, SaveAvatarRequest req) {
        Avatar a = get(ownerId, id);
        apply(a, req);
        return repository.save(a);
    }

    public void delete(UUID ownerId, UUID id) {
        repository.delete(get(ownerId, id));
    }

    private void apply(Avatar a, SaveAvatarRequest req) {
        a.setName(req.name());
        a.setBodyType(req.bodyType());
        if (req.proportions() != null) {
            a.setProportions(req.proportions().toEntity());
        } else {
            a.setProportions(BodyTypeCatalog.defaultProportions(req.bodyType()));
        }
    }
}
