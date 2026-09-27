package com.dressd.avatar.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.common.domain.BodyType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The catalog is static reference data the client renders from, so its invariants
 * (every body type has a preset, presets stay distinct, defaults are sane) are
 * worth pinning down: a typo in a preset would silently warp every silhouette.
 */
class BodyTypeCatalogTest {

    @Test
    void everyBodyTypeHasAPreset() {
        List<BodyTypeCatalog.Preset> presets = BodyTypeCatalog.all();

        assertThat(presets).extracting(BodyTypeCatalog.Preset::bodyType)
                .containsExactlyInAnyOrder(BodyType.values());
    }

    @Test
    void presetsAreSortedByBodyTypeName() {
        List<BodyTypeCatalog.Preset> presets = BodyTypeCatalog.all();

        assertThat(presets).isSortedAccordingTo(
                (a, b) -> a.bodyType().name().compareTo(b.bodyType().name()));
    }

    @Test
    void slimIsNarrowerThanCurvyAtTheShouldersAndHips() {
        Proportions slim = BodyTypeCatalog.defaultProportions(BodyType.SLIM);
        Proportions curvy = BodyTypeCatalog.defaultProportions(BodyType.CURVY);

        assertThat(slim.getShoulderWidth()).isLessThan(curvy.getShoulderWidth());
        assertThat(slim.getHipWidth()).isLessThan(curvy.getHipWidth());
    }

    @Test
    void proportionsAreReturnedByValueNotByReference() {
        Proportions first = BodyTypeCatalog.defaultProportions(BodyType.AVERAGE);
        first.setHeight(99f);

        // Mutating a returned copy must not corrupt the shared catalog.
        assertThat(BodyTypeCatalog.defaultProportions(BodyType.AVERAGE).getHeight())
                .isEqualTo(1.0f);
    }

    @Test
    void defaultProportionsMatchThePublishedPreset() {
        for (BodyTypeCatalog.Preset preset : BodyTypeCatalog.all()) {
            Proportions defaults = BodyTypeCatalog.defaultProportions(preset.bodyType());

            assertThat(defaults.getHeight()).isEqualTo(preset.proportions().getHeight());
            assertThat(defaults.getShoulderWidth()).isEqualTo(preset.proportions().getShoulderWidth());
            assertThat(defaults.getHipWidth()).isEqualTo(preset.proportions().getHipWidth());
        }
    }
}
