package com.dressd.common.auth;

import com.dressd.common.web.CurrentOwner;
import com.dressd.common.web.OwnerContext;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/** Supplies the {@link CurrentOwner} controller parameter from the request attribute. */
public class CurrentOwnerArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOwner.class)
                && UUID.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object ownerId = webRequest.getAttribute(OwnerContext.REQUEST_ATTRIBUTE,
                NativeWebRequest.SCOPE_REQUEST);
        if (ownerId instanceof UUID uuid) {
            return uuid;
        }
        // Reaching here means the handler path bypassed OwnerIdentityFilter — a
        // wiring bug, not a client error, so fail loudly rather than guessing.
        throw new IllegalStateException("No owner on request "
                + webRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                        NativeWebRequest.SCOPE_REQUEST)
                + "; is it listed in dressd.auth.public-paths?");
    }
}
