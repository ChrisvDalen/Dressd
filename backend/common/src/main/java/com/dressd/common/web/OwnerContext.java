package com.dressd.common.web;

/**
 * Shared conventions for identifying the calling user across services.
 *
 * <p>Two resolution modes exist (see {@code com.dressd.common.auth}):
 * {@code DEV} trusts the {@link #OWNER_HEADER} header for local work, and
 * {@code TOKEN} requires an HMAC-signed bearer token. Whichever mode is active,
 * the resolved owner ends up in the {@link #REQUEST_ATTRIBUTE} request
 * attribute, which is where {@link CurrentOwner} reads it from.
 */
public final class OwnerContext {

    /** Header carrying the owner's UUID. Only trusted in {@code DEV} mode. */
    public static final String OWNER_HEADER = "X-Owner-Id";

    /** Request attribute holding the resolved owner {@code UUID}. */
    public static final String REQUEST_ATTRIBUTE = "dressd.ownerId";

    private OwnerContext() {
    }
}
