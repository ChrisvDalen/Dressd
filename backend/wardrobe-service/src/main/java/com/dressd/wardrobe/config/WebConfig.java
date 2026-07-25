package com.dressd.wardrobe.config;

import com.dressd.wardrobe.storage.StorageProperties;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves stored cut-outs statically under {@code /media/**}. CORS and owner
 * authentication come from the shared {@code common} auto-configuration.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storage;

    public WebConfig(StorageProperties storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(storage.getLocal().getRoot())
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(storage.getLocal().getPublicBaseUrl() + "/**")
                .addResourceLocations(location);
    }
}
