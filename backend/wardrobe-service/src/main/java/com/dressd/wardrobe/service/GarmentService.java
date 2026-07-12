package com.dressd.wardrobe.service;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import com.dressd.wardrobe.api.NotFoundException;
import com.dressd.wardrobe.api.dto.CreateGarmentRequest;
import com.dressd.wardrobe.api.dto.UpdateGarmentRequest;
import com.dressd.wardrobe.domain.Garment;
import com.dressd.wardrobe.domain.GarmentRepository;
import com.dressd.wardrobe.domain.GarmentSpecifications;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GarmentService {

    private final GarmentRepository repository;

    public GarmentService(GarmentRepository repository) {
        this.repository = repository;
    }

    public Garment create(UUID ownerId, CreateGarmentRequest req) {
        Garment g = new Garment();
        g.setOwnerId(ownerId);
        g.setCategory(req.category());
        g.setImageUrl(req.imageUrl());
        g.setColorTag(req.colorTag());
        g.setPattern(req.pattern());
        g.setSeason(req.season());
        if (req.anchorPoints() != null) {
            g.setAnchorPoints(req.anchorPoints().toEntity());
        }
        return repository.save(g);
    }

    @Transactional(readOnly = true)
    public List<Garment> search(UUID ownerId, GarmentCategory category, String color, Season season) {
        Specification<Garment> spec = Specification.where(GarmentSpecifications.ownedBy(ownerId))
                .and(GarmentSpecifications.hasCategory(category))
                .and(GarmentSpecifications.colorMatches(color))
                .and(GarmentSpecifications.hasSeason(season));
        return repository.findAll(spec);
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
        repository.delete(g);
    }
}
