package com.dressd.common.web;

/**
 * Shared conventions for identifying the calling user across services.
 *
 * <p>v1 keeps auth deliberately simple (see SPEC.md section 8, open question on
 * auth): the owner id travels in a request header. A real identity provider
 * (Auth0 / Azure AD B2C) can later populate this header at the gateway without
 * touching downstream services.
 */
public final class OwnerContext {

    /** Header carrying the authenticated owner's UUID. */
    public static final String OWNER_HEADER = "X-Owner-Id";

    private OwnerContext() {
    }
}
