package com.dressd.avatar.domain;

import com.dressd.common.domain.BodyType;
import java.util.List;
import java.util.Map;

/**
 * Static catalog of the fixed silhouette presets (SPEC.md: avatar-service is
 * "no ML — pure static data + proportions", minimum 2-3 body types). The client
 * uses these defaults to render each silhouette.
 */
public final class BodyTypeCatalog {

    public record Preset(BodyType bodyType, String label, Proportions proportions) {
    }

    private static final Map<BodyType, Preset> PRESETS = Map.of(
            BodyType.SLIM, new Preset(BodyType.SLIM, "Slim", new Proportions(1.02f, 0.90f, 0.88f)),
            BodyType.AVERAGE, new Preset(BodyType.AVERAGE, "Average", new Proportions(1.00f, 1.00f, 1.00f)),
            BodyType.CURVY, new Preset(BodyType.CURVY, "Curvy", new Proportions(0.98f, 1.05f, 1.15f)),
            BodyType.CUSTOM, new Preset(BodyType.CUSTOM, "Custom", new Proportions(1.00f, 1.00f, 1.00f))
    );

    private BodyTypeCatalog() {
    }

    public static List<Preset> all() {
        return PRESETS.values().stream()
                .sorted((a, b) -> a.bodyType().name().compareTo(b.bodyType().name()))
                .toList();
    }

    public static Proportions defaultProportions(BodyType type) {
        Preset preset = PRESETS.get(type);
        Proportions p = preset == null ? PRESETS.get(BodyType.AVERAGE).proportions() : preset.proportions();
        return new Proportions(p.getHeight(), p.getShoulderWidth(), p.getHipWidth());
    }
}
