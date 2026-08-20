package com.dressd.wardrobe.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically reclaims scans the user never confirmed.
 *
 * <p>Set {@code dressd.storage.pending-cleanup-enabled=false} to switch the sweep
 * off, which is what the test profile does so tests are not racing a background
 * job over the same directory.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "dressd.storage", name = "pending-cleanup-enabled",
        havingValue = "true", matchIfMissing = true)
public class PendingScanCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PendingScanCleanupJob.class);

    private final GarmentImages images;
    private final StorageProperties properties;

    public PendingScanCleanupJob(GarmentImages images, StorageProperties properties) {
        this.images = images;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${dressd.storage.pending-cleanup-interval:PT1H}",
            initialDelayString = "${dressd.storage.pending-cleanup-interval:PT1H}")
    public void purge() {
        try {
            images.purgeStalePending(properties.getPendingTtl());
        } catch (RuntimeException e) {
            // A failed sweep must not kill the scheduler thread.
            log.warn("Pending scan cleanup failed: {}", e.getMessage());
        }
    }
}
