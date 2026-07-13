package com.dressd.wardrobe.domain;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable filters for the wardrobe grid (SPEC.md flow C: filter on
 * category / colour / season).
 */
public final class GarmentSpecifications {

    private GarmentSpecifications() {
    }

    public static Specification<Garment> ownedBy(UUID ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<Garment> hasCategory(GarmentCategory category) {
        if (category == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Garment> hasSeason(Season season) {
        if (season == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("season"), season);
    }

    /** Case-insensitive substring match on the colour tag (e.g. "blue" or "#33"). */
    public static Specification<Garment> colorMatches(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String needle = "%" + color.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("colorTag")), needle);
    }
}
