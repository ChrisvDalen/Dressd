package com.dressd.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * The page-size clamp is a security-relevant bound: it is what stops a client
 * from asking a service to materialise an unbounded result set.
 */
class PageRequestsTest {

    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "updatedAt");

    @Test
    void defaultsWhenNothingIsSupplied() {
        Pageable pageable = PageRequests.of(null, null, SORT);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(PageRequests.DEFAULT_SIZE);
    }

    @Test
    void clampsASizeAboveTheMaximum() {
        assertThat(PageRequests.of(0, 10_000_000, SORT).getPageSize()).isEqualTo(PageRequests.MAX_SIZE);
    }

    @Test
    void honoursAReasonableSize() {
        assertThat(PageRequests.of(0, 10, SORT).getPageSize()).isEqualTo(10);
    }

    @Test
    void treatsANonPositiveSizeAsTheDefault() {
        assertThat(PageRequests.of(0, 0, SORT).getPageSize()).isEqualTo(PageRequests.DEFAULT_SIZE);
        assertThat(PageRequests.of(0, -5, SORT).getPageSize()).isEqualTo(PageRequests.DEFAULT_SIZE);
    }

    @Test
    void treatsANegativePageAsTheFirstPage() {
        assertThat(PageRequests.of(-3, null, SORT).getPageNumber()).isZero();
    }

    @Test
    void keepsTheRequestedSortAndPage() {
        Pageable pageable = PageRequests.of(2, 10, SORT);

        assertThat(pageable.getSort()).isEqualTo(SORT);
        assertThat(pageable.getPageNumber()).isEqualTo(2);
    }
}
