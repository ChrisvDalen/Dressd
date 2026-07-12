package com.dressd.outfit.service;

import com.dressd.outfit.api.NotFoundException;
import com.dressd.outfit.api.dto.GarmentLayerDto;
import com.dressd.outfit.api.dto.SaveOutfitRequest;
import com.dressd.outfit.domain.GarmentLayer;
import com.dressd.outfit.domain.Outfit;
import com.dressd.outfit.domain.OutfitRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OutfitService {

    private final OutfitRepository repository;

    public OutfitService(OutfitRepository repository) {
        this.repository = repository;
    }

    public Outfit create(UUID ownerId, SaveOutfitRequest req) {
        Outfit o = new Outfit();
        o.setOwnerId(ownerId);
        apply(o, req);
        return repository.save(o);
    }

    @Transactional(readOnly = true)
    public List<Outfit> list(UUID ownerId) {
        return repository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public Outfit get(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Outfit not found: " + id));
    }

    public Outfit update(UUID ownerId, UUID id, SaveOutfitRequest req) {
        Outfit o = get(ownerId, id);
        apply(o, req);
        return repository.save(o);
    }

    public void delete(UUID ownerId, UUID id) {
        repository.delete(get(ownerId, id));
    }

    private void apply(Outfit o, SaveOutfitRequest req) {
        o.setAvatarId(req.avatarId());
        o.setName(req.name());
        List<GarmentLayer> layers = new ArrayList<>();
        if (req.garmentLayers() != null) {
            for (GarmentLayerDto dto : req.garmentLayers()) {
                layers.add(dto.toEntity());
            }
        }
        o.getGarmentLayers().clear();
        o.getGarmentLayers().addAll(layers);
    }
}
