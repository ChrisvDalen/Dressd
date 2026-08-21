package com.dressd.wardrobe.service;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import com.dressd.common.web.NotFoundException;
import com.dressd.wardrobe.api.dto.CreateGarmentRequest;
import com.dressd.wardrobe.api.dto.UpdateGarmentRequest;
import com.dressd.wardrobe.domain.Garment;
import com.dressd.wardrobe.domain.GarmentRepository;
import com.dressd.wardrobe.domain.GarmentSpecifications;
import com.dressd.wardrobe.storage.GarmentImages;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GarmentService {

    private final GarmentRepository repository;
    private final GarmentImages images;

    public GarmentService(GarmentRepository repository, GarmentImages images) {
        this.repository = repository;
        this.images = images;
    }

    public Garment create(UUID ownerId, CreateGarmentRequest req) {
        Garment g = new Garment();
        g.setOwnerId(ownerId);
        g.setCategory(req.category());
        // Confirming moves the scan out of the pending area and rejects image URLs
        // that are not this owner's.
        g.setImageUrl(images.confirm(ownerId, req.imageUrl()));
        g.setColorTag(req.colorTag());
        g.setPattern(req.pattern());
        g.setSeason(req.season());
        if (req.anchorPoints() != null) {
            g.setAnchorPoints(req.anchorPoints().toEntity());
        }
        return repository.save(g);
    }

    @Transactional(readOnly = true)
    public Page<Garment> search(UUID ownerId, GarmentCategory category, String color, Season season,
                                Pageable pageable) {
        Specification<Garment> spec = GarmentSpecifications.ownedBy(ownerId)
                .and(GarmentSpecifications.hasCategory(category))
                .and(GarmentSpecifications.colorMatches(color))
                .and(GarmentSpecifications.hasSeason(season));
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Garment get(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Garment not found: " + id));
    }

    public Garment update(UUID ownerId, UUID id, UpdateGarmentRequest req) {
        Garment g = get(ownerId, id);
        if (req.category() != null) {
            g.setCategory(req.category());
        }
        if (req.colorTag() != null) {
            g.setColorTag(req.colorTag());
        }
        if (req.pattern() != null) {
            g.setPattern(req.pattern());
        }
        if (req.season() != null) {
            g.setSeason(req.season());
        }
        if (req.anchorPoints() != null) {
            g.setAnchorPoints(req.anchorPoints().toEntity());
        }
        return repository.save(g);
    }

    public void delete(UUID ownerId, UUID id) {
        Garment g = get(ownerId, id);
        String imageUrl = g.getImageUrl();
        repository.delete(g);
        // Only once the row is gone do we drop the bytes, so a failed delete never
        // leaves a garment pointing at a missing image.
        images.discard(ownerId, imageUrl);
    }
}
