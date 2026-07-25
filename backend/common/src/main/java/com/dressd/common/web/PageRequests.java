package com.dressd.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds {@link Pageable}s from raw query parameters, clamping the page size so a
 * client cannot ask a service to materialise an unbounded result set.
 */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    private PageRequests() {
    }

    public static Pageable of(Integer page, Integer size, Sort sort) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }
}
