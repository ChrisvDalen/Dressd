package com.dressd.wardrobe.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes objects to a local directory that is served statically under
 * {@code /media/**} (see {@link com.dressd.wardrobe.config.WebConfig}). Suitable
 * for local dev and CI; swap for an Azure Blob / S3 implementation in production.
 */
@Component
public class LocalObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalObjectStorage.class);

    private final Path root;
    private final String publicBaseUrl;

    public LocalObjectStorage(StorageProperties properties) {
        this.root = Path.of(properties.getLocal().getRoot()).toAbsolutePath().normalize();
        String base = properties.getLocal().getPublicBaseUrl();
        this.publicBaseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        try {
            Files.createDirectories(this.root);
            log.info("Local object storage rooted at {}", this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root " + this.root, e);
        }
    }

    @Override
    public String store(String key, byte[] bytes, String contentType) {
        String safeKey = normalizeKey(key);
        Path target = resolve(safeKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store object " + safeKey, e);
        }
        return urlFor(safeKey);
    }

    @Override
    public String move(String sourceKey, String targetKey) {
        String safeSource = normalizeKey(sourceKey);
        String safeTarget = normalizeKey(targetKey);
        Path source = resolve(safeSource);
        Path target = resolve(safeTarget);
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("Object already exists at " + safeTarget, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to move " + safeSource + " to " + safeTarget, e);
        }
        return urlFor(safeTarget);
    }

    @Override
    public boolean delete(String key) {
        Path target = resolve(normalizeKey(key));
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            // A leaked file is not worth failing the caller's request over.
            log.warn("Could not delete stored object {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<String> keyForUrl(String url) {
        if (url == null) {
            return Optional.empty();
        }
        String prefix = publicBaseUrl + "/";
        if (!url.startsWith(prefix)) {
            return Optional.empty();
        }
        String key = url.substring(prefix.length());
        if (key.isBlank() || hasTraversal(key)) {
            return Optional.empty();
        }
        return Optional.of(key);
    }

    @Override
    public List<String> keysModifiedBefore(String prefix, Instant cutoff) {
        Path base = resolve(normalizeKey(prefix));
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(base)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> lastModifiedBefore(path, cutoff))
                    .map(path -> root.relativize(path).toString().replace('\\', '/'))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            log.warn("Could not scan {} for stale objects: {}", prefix, e.getMessage());
            return List.of();
        }
    }

    private static boolean lastModifiedBefore(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException e) {
            return false;
        }
    }

    /** Strips leading slashes and rejects anything trying to climb out of the root. */
    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be empty");
        }
        String stripped = key.replaceAll("^/+", "");
        if (hasTraversal(stripped)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return stripped;
    }

    private static boolean hasTraversal(String key) {
        for (String segment : key.split("/")) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private Path resolve(String safeKey) {
        Path target = root.resolve(safeKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + safeKey);
        }
        return target;
    }

    private String urlFor(String safeKey) {
        return publicBaseUrl + "/" + safeKey;
    }

    Path getRoot() {
        return root;
    }
}
