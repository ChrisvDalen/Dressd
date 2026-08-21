package com.dressd.common.auth;

import com.dressd.common.web.ApiError;
import com.dressd.common.web.BadRequestException;
import com.dressd.common.web.OwnerContext;
import com.dressd.common.web.UnauthorizedException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the calling owner once per request and stores it in
 * {@link OwnerContext#REQUEST_ATTRIBUTE}, rejecting the request outright when the
 * credential is missing or invalid.
 */
public class OwnerIdentityFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final OwnerResolver resolver;
    private final OwnerAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public OwnerIdentityFilter(OwnerResolver resolver, OwnerAuthProperties properties, ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // CORS preflight carries no credentials by definition, and the public
        // paths (actuator probes, static media) are unauthenticated on purpose.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        for (String pattern : properties.getPublicPaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID ownerId;
        try {
            ownerId = resolver.resolve(bearerToken(request), request.getHeader(OwnerContext.OWNER_HEADER));
        } catch (UnauthorizedException e) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            writeError(request, response, HttpStatus.UNAUTHORIZED, e.getMessage());
            return;
        } catch (BadRequestException e) {
            writeError(request, response, HttpStatus.BAD_REQUEST, e.getMessage());
            return;
        }

        request.setAttribute(OwnerContext.REQUEST_ATTRIBUTE, ownerId);
        chain.doFilter(request, response);
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        return header.substring(BEARER.length()).trim();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
