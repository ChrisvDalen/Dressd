package com.dressd.common.auth;

import com.dressd.common.web.BadRequestException;
import com.dressd.common.web.UnauthorizedException;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Decides which owner a request belongs to, independent of the servlet stack so
 * the rules can be unit-tested directly.
 *
 * <p>In {@link OwnerAuthProperties.Mode#TOKEN} mode only a valid signed token is
 * accepted and the {@code X-Owner-Id} header is ignored outright — otherwise the
 * header would be a trivial bypass of the very thing the token proves.
 */
public class OwnerResolver {

    private final OwnerAuthProperties properties;
    private final OwnerTokenService tokenService;

    private OwnerResolver(OwnerAuthProperties properties, @Nullable OwnerTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    /**
     * Builds a resolver for the given configuration.
     *
     * @throws IllegalStateException in TOKEN mode when no usable secret is set, so
     *                               a misconfigured deployment fails at startup
     *                               instead of serving unauthenticated traffic
     */
    public static OwnerResolver from(OwnerAuthProperties properties) {
        String secret = properties.getSecret();
        boolean usableSecret = secret != null && secret.length() >= OwnerAuthProperties.MIN_SECRET_LENGTH;

        if (properties.getMode() == OwnerAuthProperties.Mode.TOKEN && !usableSecret) {
            throw new IllegalStateException(
                    "dressd.auth.mode=TOKEN requires dressd.auth.secret of at least "
                            + OwnerAuthProperties.MIN_SECRET_LENGTH + " characters. "
                            + "Set the AUTH_SECRET environment variable, or use dressd.auth.mode=DEV "
                            + "for local development.");
        }
        OwnerTokenService tokens = usableSecret
                ? new OwnerTokenService(secret, properties.getTokenTtl())
                : null;
        return new OwnerResolver(properties, tokens);
    }

    /**
     * @param bearerToken the {@code Authorization: Bearer} value, or {@code null}
     * @param ownerHeader the {@code X-Owner-Id} value, or {@code null}
     */
    public UUID resolve(@Nullable String bearerToken, @Nullable String ownerHeader) {
        if (properties.getMode() == OwnerAuthProperties.Mode.TOKEN) {
            if (isBlank(bearerToken)) {
                throw new UnauthorizedException("Missing bearer token");
            }
            return verify(bearerToken);
        }

        // DEV mode: honour a token when one is offered so token flows can be
        // exercised locally, otherwise trust the header, otherwise demo owner.
        if (!isBlank(bearerToken) && tokenService != null) {
            return verify(bearerToken);
        }
        if (isBlank(ownerHeader)) {
            return properties.getDemoOwner();
        }
        try {
            return UUID.fromString(ownerHeader.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid " + com.dressd.common.web.OwnerContext.OWNER_HEADER + " header: not a UUID");
        }
    }

    /** The token minter/verifier, or {@code null} when no secret is configured. */
    @Nullable
    public OwnerTokenService tokenService() {
        return tokenService;
    }

    private UUID verify(String bearerToken) {
        if (tokenService == null) {
            throw new UnauthorizedException("Token authentication is not configured");
        }
        try {
            return tokenService.verify(bearerToken);
        } catch (OwnerTokenService.InvalidTokenException e) {
            throw new UnauthorizedException(e.getMessage());
        }
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
