package com.dressd.wardrobe.recognition;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the garment-recognition sidecar (SPEC.md section 3).
 */
@ConfigurationProperties(prefix = "dressd.recognition")
public class RecognitionProperties {

    /** Base URL of the FastAPI recognition service. */
    private String baseUrl = "http://localhost:8000";

    /**
     * When false the wardrobe service skips the network call and returns a
     * deterministic local stub. Handy for tests and offline development.
     */
    private boolean enabled = true;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
