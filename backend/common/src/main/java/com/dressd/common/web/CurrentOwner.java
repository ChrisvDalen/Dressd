package com.dressd.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated owner's {@code UUID} into a controller method.
 *
 * <p>The value is resolved once per request by
 * {@code com.dressd.common.auth.OwnerIdentityFilter}, so controllers never see
 * raw headers or tokens:
 *
 * <pre>{@code
 * @GetMapping
 * List<GarmentResponse> list(@CurrentOwner UUID ownerId) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentOwner {
}
