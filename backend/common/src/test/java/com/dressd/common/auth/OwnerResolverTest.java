package com.dressd.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dressd.common.auth.OwnerAuthProperties.Mode;
import com.dressd.common.web.BadRequestException;
import com.dressd.common.web.UnauthorizedException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OwnerResolverTest {

    private static final String SECRET = "test-secret-that-is-long-enough-32";
    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static OwnerAuthProperties properties(Mode mode, String secret) {
        OwnerAuthProperties props = new OwnerAuthProperties();
        props.setMode(mode);
        props.setSecret(secret);
        return props;
    }

    @Test
    void devModeFallsBackToDemoOwner() {
        OwnerResolver resolver = OwnerResolver.from(properties(Mode.DEV, ""));
        OwnerAuthProperties defaults = new OwnerAuthProperties();

        assertThat(resolver.resolve(null, null)).isEqualTo(defaults.getDemoOwner());
    }

    @Test
    void devModeTrustsOwnerHeader() {
        OwnerResolver resolver = OwnerResolver.from(properties(Mode.DEV, ""));

        assertThat(resolver.resolve(null, OWNER.toString())).isEqualTo(OWNER);
    }

    @Test
    void devModeRejectsMalformedOwnerHeaderAsBadRequest() {
        OwnerResolver resolver = OwnerResolver.from(properties(Mode.DEV, ""));

        assertThatThrownBy(() -> resolver.resolve(null, "not-a-uuid"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a UUID");
    }

    @Test
    void tokenModeIgnoresOwnerHeaderEntirely() {
        OwnerResolver resolver = OwnerResolver.from(properties(Mode.TOKEN, SECRET));

        assertThatThrownBy(() -> resolver.resolve(null, OWNER.toString()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing bearer token");
    }

    @Test
    void tokenModeAcceptsAValidToken() {
        OwnerResolver resolver = OwnerResolver.from(properties(Mode.TOKEN, SECRET));
        String token = resolver.tokenService().mint(OWNER);

        assertThat(resolver.resolve(token, null)).isEqualTo(OWNER);
    }

    @Test
    void tokenModeRejectsAnInvalidToken() {
        OwnerResolver resolver = OwnerResolver.from(properties(Mode.TOKEN, SECRET));

        assertThatThrownBy(() -> resolver.resolve("v1.bogus.bogus", null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void tokenModeWithoutASecretFailsFast() {
        assertThatThrownBy(() -> OwnerResolver.from(properties(Mode.TOKEN, "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dressd.auth.secret");
    }
}
