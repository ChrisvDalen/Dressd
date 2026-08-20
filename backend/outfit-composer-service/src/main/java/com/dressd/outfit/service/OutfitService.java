package com.dressd.outfit.service;

import com.dressd.common.web.BadRequestException;
import com.dressd.common.web.NotFoundException;
import com.dressd.outfit.api.dto.GarmentLayerDto;
import com.dressd.outfit.api.dto.SaveOutfitRequest;
import com.dressd.outfit.domain.GarmentLayer;
import com.dressd.outfit.domain.Outfit;
import com.dressd.outfit.domain.OutfitRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OutfitService {

    /** Guards against a client stacking an unbounded number of layers. */
    private static final int MAX_LAYERS = 32;

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
    public Page<Outfit> list(UUID ownerId, Pageable pageable) {
        return repository.findByOwnerId(ownerId, pageable);
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

        List<GarmentLayer> layers = normalize(req.garmentLayers());
        o.getGarmentLayers().clear();
        o.getGarmentLayers().addAll(layers);
    }

    /**
     * Rejects structurally impossible layer stacks and renumbers z-indexes into a
     * contiguous 0..n-1 range, so stored outfits always render deterministically
     * regardless of what the client sent.
     */
    private static List<GarmentLayer> normalize(List<GarmentLayerDto> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        if (requested.size() > MAX_LAYERS) {
            throw new BadRequestException("An outfit cannot have more than " + MAX_LAYERS + " layers");
        }

        Set<UUID> seenGarments = new HashSet<>();
        for (GarmentLayerDto dto : requested) {
            if (!seenGarments.add(dto.garmentId())) {
                throw new BadRequestException("Garment " + dto.garmentId() + " appears twice in the outfit");
            }
        }

        List<GarmentLayerDto> ordered = new ArrayList<>(requested);
        ordered.sort(Comparator.comparingInt(GarmentLayerDto::zIndex));

        List<GarmentLayer> layers = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            layers.add(ordered.get(i).toEntity(i));
        }
        return layers;
    }
}
