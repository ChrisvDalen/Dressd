package com.dressd.wardrobe.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for where cut-out images live ({@code dressd.storage}). */
@ConfigurationProperties(prefix = "dressd.storage")
public class StorageProperties {

    private final Local local = new Local();

    /**
     * How long an unconfirmed scan stays in the pending area before the cleanup
     * job reclaims it. Users abandon scans, and those bytes are nobody's.
     */
    private Duration pendingTtl = Duration.ofHours(6);

    /** How often to sweep the pending area. */
    private Duration pendingCleanupInterval = Duration.ofHours(1);

    /** Whether to run the pending-scan sweep at all. */
    private boolean pendingCleanupEnabled = true;

    public Local getLocal() {
        return local;
    }

    public Duration getPendingTtl() {
        return pendingTtl;
    }

    public void setPendingTtl(Duration pendingTtl) {
        this.pendingTtl = pendingTtl;
    }

    public Duration getPendingCleanupInterval() {
        return pendingCleanupInterval;
    }

    public void setPendingCleanupInterval(Duration pendingCleanupInterval) {
        this.pendingCleanupInterval = pendingCleanupInterval;
    }

    public boolean isPendingCleanupEnabled() {
        return pendingCleanupEnabled;
    }

    public void setPendingCleanupEnabled(boolean pendingCleanupEnabled) {
        this.pendingCleanupEnabled = pendingCleanupEnabled;
    }

    public static class Local {

        /** Filesystem directory backing the store. */
        private String root = "./data/media";

        /** URL prefix the stored objects are served under. */
        private String publicBaseUrl = "/media";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }
}
