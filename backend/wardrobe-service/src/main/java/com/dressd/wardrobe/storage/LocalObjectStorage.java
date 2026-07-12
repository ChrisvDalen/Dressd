package com.dressd.wardrobe.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Writes objects to a local directory that is served statically under
 * {@code /media/**} (see {@code StaticResourceConfig}). Suitable for local dev
 * and CI; swap for an Azure Blob / S3 implementation in production.
 */
@Component
public class LocalObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalObjectStorage.class);

    private final Path root;
    private final String publicBaseUrl;

    public LocalObjectStorage(
            @Value("${dressd.storage.local.root:./data/media}") String root,
            @Value("${dressd.storage.local.public-base-url:/media}") String publicBaseUrl) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        try {
            Files.createDirectories(this.root);
            log.info("Local object storage rooted at {}", this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root " + this.root, e);
        }
    }

    @Override
    public String store(String key, byte[] bytes, String contentType) {
        String safeKey = key.replaceAll("^/+", "");
        Path target = root.resolve(safeKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store object " + key, e);
        }
        return publicBaseUrl + "/" + safeKey;
    }

    Path getRoot() {
        return root;
    }
}
