package com.dressd.common.auth;

import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for how the calling owner is authenticated ({@code dressd.auth}).
 *
 * <p>The default is {@link Mode#DEV} so a freshly cloned checkout runs without
 * any setup. The {@code prod} profile of every service flips this to
 * {@link Mode#TOKEN}, and TOKEN mode refuses to start without a secret — so a
 * real deployment that forgets to configure auth fails closed rather than
 * silently trusting a client-supplied header.
 */
@ConfigurationProperties(prefix = "dressd.auth")
public class OwnerAuthProperties {

    public enum Mode {
        /**
         * Trusts the {@code X-Owner-Id} header and falls back to {@link #demoOwner}.
         * Convenient locally, unauthenticated by design — never use in production.
         */
        DEV,
        /** Requires a valid {@code Authorization: Bearer <token>} on every API call. */
        TOKEN
    }

    /** Minimum secret length; 32 bytes matches the HMAC-SHA256 output size. */
    public static final int MIN_SECRET_LENGTH = 32;

    private Mode mode = Mode.DEV;

    /** Shared HMAC secret used to sign and verify owner tokens. */
    private String secret = "";

    /** Owner used in {@link Mode#DEV} when no header is supplied. */
    private UUID demoOwner = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** How long a freshly minted token stays valid. */
    private Duration tokenTtl = Duration.ofHours(24);

    /**
     * Request paths exempt from authentication. Actuator probes and the static
     * media handler are public; everything under {@code /api} is not.
     */
    private String[] publicPaths = {"/actuator/**", "/media/**", "/error"};

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public UUID getDemoOwner() {
        return demoOwner;
    }

    public void setDemoOwner(UUID demoOwner) {
        this.demoOwner = demoOwner;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String[] getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(String[] publicPaths) {
        this.publicPaths = publicPaths;
    }
}
