package com.dressd.wardrobe.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Exercises the composable grid filters end-to-end against the real database
 * (SPEC.md flow C: filter on category / colour / season), including the
 * "null means unfiltered" contract the UI relies on.
 */
@SpringBootTest
class GarmentSpecificationsRepositoryTest {

    @Autowired
    private GarmentRepository repository;

    @Test
    void ownedByReturnsOnlyThatOwnersGarments() {
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();
        repository.save(garment(mine, "A", GarmentCategory.TOP, null));
        repository.save(garment(mine, "B", GarmentCategory.TOP, null));
        repository.save(garment(theirs, "C", GarmentCategory.TOP, null));

        assertThat(repository.findAll(GarmentSpecifications.ownedBy(mine))).hasSize(2);
        assertThat(repository.findAll(GarmentSpecifications.ownedBy(theirs))).hasSize(1);
        assertThat(repository.findAll(GarmentSpecifications.ownedBy(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void hasCategoryFiltersButNullMeansUnrestricted() {
        UUID owner = UUID.randomUUID();
        repository.save(garment(owner, "T", GarmentCategory.TOP, null));
        repository.save(garment(owner, "S", GarmentCategory.SHOES, null));

        assertThat(categories(owner, GarmentSpecifications.hasCategory(GarmentCategory.SHOES)))
                .containsExactly(GarmentCategory.SHOES);
        // Null is the "no filter" sentinel, not "match the NULL category".
        assertThat(categories(owner, GarmentSpecifications.hasCategory(null))).hasSize(2);
    }

    @Test
    void hasSeasonFiltersButNullMeansUnrestricted() {
        UUID owner = UUID.randomUUID();
        repository.save(garment(owner, "S", GarmentCategory.TOP, Season.SUMMER));
        repository.save(garment(owner, "W", GarmentCategory.TOP, Season.WINTER));

        assertThat(seasons(owner, GarmentSpecifications.hasSeason(Season.WINTER)))
                .containsExactly(Season.WINTER);
        assertThat(seasons(owner, GarmentSpecifications.hasSeason(null))).hasSize(2);
    }

    @Test
    void colorMatchesIsCaseInsensitiveAndPartial() {
        UUID owner = UUID.randomUUID();
        repository.save(garment(owner, "#3A5FCD", GarmentCategory.TOP, null));
        repository.save(garment(owner, "#999999", GarmentCategory.TOP, null));

        // A user types a fragment, in any case.
        assertThat(names(owner, GarmentSpecifications.colorMatches("3a5f"))).containsExactly("#3A5FCD");
        assertThat(names(owner, GarmentSpecifications.colorMatches("3A5FCD"))).containsExactly("#3A5FCD");
        assertThat(names(owner, GarmentSpecifications.colorMatches("#999"))).containsExactly("#999999");
        // Blank and null are "no filter", not an empty search.
        assertThat(names(owner, GarmentSpecifications.colorMatches("  "))).hasSize(2);
        assertThat(names(owner, GarmentSpecifications.colorMatches(null))).hasSize(2);
    }

    @Test
    void predicatesComposeWithAnd() {
        UUID owner = UUID.randomUUID();
        repository.save(garment(owner, "blue-top", GarmentCategory.TOP, null));
        repository.save(garment(owner, "blue-shoes", GarmentCategory.SHOES, null));
        repository.save(garment(owner, "red-top", GarmentCategory.TOP, null));

        Specification<Garment> spec = GarmentSpecifications.ownedBy(owner)
                .and(GarmentSpecifications.hasCategory(GarmentCategory.TOP))
                .and(GarmentSpecifications.colorMatches("blue"));

        assertThat(names(owner, spec)).containsExactly("blue-top");
    }

    private Garment garment(UUID owner, String color, GarmentCategory category, Season season) {
        Garment g = new Garment();
        g.setOwnerId(owner);
        g.setColorTag(color);
        g.setCategory(category);
        if (season != null) {
            g.setSeason(season);
        }
        g.setImageUrl("/media/pending/" + owner + "/" + UUID.randomUUID() + ".png");
        return g;
    }

    private List<String> names(UUID owner, Specification<Garment> spec) {
        return repository.findAll(GarmentSpecifications.ownedBy(owner).and(spec)).stream()
                .map(Garment::getColorTag).sorted().collect(Collectors.toList());
    }

    private List<GarmentCategory> categories(UUID owner, Specification<Garment> spec) {
        return repository.findAll(GarmentSpecifications.ownedBy(owner).and(spec)).stream()
                .map(Garment::getCategory).toList();
    }

    private List<Season> seasons(UUID owner, Specification<Garment> spec) {
        return repository.findAll(GarmentSpecifications.ownedBy(owner).and(spec)).stream()
                .map(Garment::getSeason).toList();
    }
}
