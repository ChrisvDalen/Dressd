package com.dressd.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cross-origin configuration shared by all services ({@code dressd.cors}). */
@ConfigurationProperties(prefix = "dressd.cors")
public class DressdCorsProperties {

    /** Origins allowed to call the API. Defaults to the Angular dev server. */
    private String[] allowedOrigins = {"http://localhost:4200"};

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
