package com.dressd.common.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mints and verifies the compact owner tokens used in
 * {@link OwnerAuthProperties.Mode#TOKEN} mode.
 *
 * <p>Format: {@code v1.<base64url(ownerId:expiryEpochSeconds)>.<base64url(hmacSha256)>}.
 * The MAC covers the {@code v1.<payload>} prefix, so neither the version marker
 * nor the payload can be altered. A single fixed algorithm is baked in, which
 * removes the algorithm-confusion class of bugs that generic JWT parsing invites.
 *
 * <p>This is deliberately a placeholder for a real identity provider: an API
 * gateway fronting Auth0 / Entra ID can mint the same token — or the services can
 * be switched to validating the IdP's JWTs — without controllers changing.
 */
public class OwnerTokenService {

    private static final String PREFIX = "v1.";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration tokenTtl;

    public OwnerTokenService(String secret, Duration tokenTtl) {
        if (secret == null || secret.length() < OwnerAuthProperties.MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    "dressd.auth.secret must be at least " + OwnerAuthProperties.MIN_SECRET_LENGTH
                            + " characters to sign owner tokens");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenTtl = tokenTtl;
    }

    /** Mints a token for {@code ownerId} that expires after the configured TTL. */
    public String mint(UUID ownerId) {
        return mint(ownerId, Instant.now().plus(tokenTtl));
    }

    public String mint(UUID ownerId, Instant expiresAt) {
        String payload = ownerId + ":" + expiresAt.getEpochSecond();
        String signingInput = PREFIX + ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + ENCODER.encodeToString(hmac(signingInput));
    }

    /**
     * Verifies a token and returns the owner it belongs to.
     *
     * @throws InvalidTokenException if the token is malformed, has a bad
     *                               signature, or has expired
     */
    public UUID verify(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            throw new InvalidTokenException("Malformed token");
        }
        int lastDot = token.lastIndexOf('.');
        if (lastDot < PREFIX.length()) {
            throw new InvalidTokenException("Malformed token");
        }
        String signingInput = token.substring(0, lastDot);
        byte[] presented;
        byte[] payloadBytes;
        try {
            presented = DECODER.decode(token.substring(lastDot + 1));
            payloadBytes = DECODER.decode(signingInput.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Malformed token");
        }

        // Constant-time comparison: a length-or-content shortcut here would leak
        // the expected MAC one byte at a time.
        if (!MessageDigest.isEqual(hmac(signingInput), presented)) {
            throw new InvalidTokenException("Bad token signature");
        }

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        int separator = payload.lastIndexOf(':');
        if (separator < 0) {
            throw new InvalidTokenException("Malformed token payload");
        }
        UUID ownerId;
        long expiry;
        try {
            ownerId = UUID.fromString(payload.substring(0, separator));
            expiry = Long.parseLong(payload.substring(separator + 1));
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Malformed token payload");
        }
        if (Instant.now().getEpochSecond() > expiry) {
            throw new InvalidTokenException("Token expired");
        }
        return ownerId;
    }

    private byte[] hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    /** Thrown when a presented token cannot be trusted. */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
