package com.dressd.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dressd.common.auth.OwnerTokenService.InvalidTokenException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OwnerTokenServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-32";
    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final OwnerTokenService tokens = new OwnerTokenService(SECRET, Duration.ofHours(1));

    @Test
    void mintedTokenVerifiesBackToTheSameOwner() {
        assertThat(tokens.verify(tokens.mint(OWNER))).isEqualTo(OWNER);
    }

    @Test
    void rejectsShortSecrets() {
        assertThatThrownBy(() -> new OwnerTokenService("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        String foreign = new OwnerTokenService("a-completely-different-secret-3232", Duration.ofHours(1))
                .mint(OWNER);

        assertThatThrownBy(() -> tokens.verify(foreign))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void rejectsTamperedPayload() {
        String token = tokens.mint(OWNER);
        String[] parts = token.split("\\.");
        // Swap the payload for one naming a different owner, keeping the old MAC.
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                (UUID.randomUUID() + ":" + Instant.now().plusSeconds(3600).getEpochSecond())
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> tokens.verify(parts[0] + "." + forgedPayload + "." + parts[2]))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String expired = tokens.mint(OWNER, Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> tokens.verify(expired))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> tokens.verify("not-a-token")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> tokens.verify("v1.")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> tokens.verify(null)).isInstanceOf(InvalidTokenException.class);
    }
}
