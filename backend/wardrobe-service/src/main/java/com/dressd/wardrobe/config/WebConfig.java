package com.dressd.wardrobe.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves stored PNGs statically under {@code /media/**} and enables CORS for
 * the Angular dev server.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String storageRoot;
    private final String allowedOrigins;

    public WebConfig(
            @Value("${dressd.storage.local.root:./data/media}") String storageRoot,
            @Value("${dressd.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.storageRoot = storageRoot;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(storageRoot).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
