package com.dressd.common.auth;

import java.time.Duration;
import java.util.UUID;

/**
 * Mints an owner token from the command line, for exercising {@code TOKEN} mode
 * without an identity provider in front of the services:
 *
 * <pre>{@code
 * java -cp backend/common/target/classes com.dressd.common.auth.OwnerTokenTool \
 *     "<secret>" 00000000-0000-0000-0000-000000000001 [ttlHours]
 * }</pre>
 */
public final class OwnerTokenTool {

    private OwnerTokenTool() {
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: OwnerTokenTool <secret> <ownerUuid> [ttlHours]");
            System.exit(2);
            return;
        }
        Duration ttl = Duration.ofHours(args.length > 2 ? Long.parseLong(args[2]) : 24L);
        System.out.println(new OwnerTokenService(args[0], ttl).mint(UUID.fromString(args[1])));
    }
}
