package com.dressd.avatar.api;

import java.util.UUID;

public final class OwnerId {

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
