package com.dressd.wardrobe.api;

import java.util.UUID;

/**
 * Resolves the calling owner id. v1 reads it from the {@code X-Owner-Id}
 * header and falls back to a fixed demo owner when absent, so the app is
 * usable before a real identity provider is wired in (SPEC.md section 8).
 */
public final class OwnerId {

    /** Stable demo owner used when no header is supplied. */
    public static final UUID DEMO_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private OwnerId() {
    }

    public static UUID resolve(String header) {
        if (header == null || header.isBlank()) {
            return DEMO_OWNER;
        }
        return UUID.fromString(header);
    }
}
