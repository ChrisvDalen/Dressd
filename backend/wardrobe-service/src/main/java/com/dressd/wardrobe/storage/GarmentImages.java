package com.dressd.wardrobe.storage;

import com.dressd.common.web.BadRequestException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Owns the lifecycle of a garment's cut-out image.
 *
 * <p>A scan lands in {@code pending/<owner>/} and is only promoted to
 * {@code garments/<owner>/} once the user confirms it, so abandoned scans are
 * separable from real wardrobe content and can be swept up later. Promotion also
 * doubles as the authorisation check on the client-supplied {@code imageUrl}:
 * without it a caller could attach any URL — including another owner's image — to
 * their own garment.
 */
@Component
public class GarmentImages {

    static final String PENDING_PREFIX = "pending/";
    static final String GARMENTS_PREFIX = "garments/";

    private static final Logger log = LoggerFactory.getLogger(GarmentImages.class);

    private final ObjectStorage storage;

    public GarmentImages(ObjectStorage storage) {
        this.storage = storage;
    }

    /** Key a freshly scanned, not-yet-confirmed image should be written to. */
    public String pendingKey(UUID ownerId) {
        return PENDING_PREFIX + ownerId + "/" + UUID.randomUUID() + ".png";
    }

    /**
     * Validates that {@code imageUrl} is one of {@code ownerId}'s own images and
     * moves it out of the pending area.
     *
     * @return the URL the garment should store
     * @throws BadRequestException if the URL is not this owner's scan
     */
    public String confirm(UUID ownerId, String imageUrl) {
        String key = storage.keyForUrl(imageUrl)
                .orElseThrow(() -> new BadRequestException("imageUrl is not a Dressd media URL"));

        // Already-confirmed images pass through so updates stay idempotent.
        if (key.startsWith(GARMENTS_PREFIX + ownerId + "/")) {
            return imageUrl;
        }

        String pendingPrefix = PENDING_PREFIX + ownerId + "/";
        if (!key.startsWith(pendingPrefix)) {
            throw new BadRequestException("imageUrl does not reference one of your scans");
        }
        String filename = key.substring(pendingPrefix.length());
        return storage.move(key, GARMENTS_PREFIX + ownerId + "/" + filename);
    }

    /** Deletes a garment's image. Best-effort: never fails the caller's request. */
    public void discard(UUID ownerId, String imageUrl) {
        storage.keyForUrl(imageUrl)
                .filter(key -> key.startsWith(GARMENTS_PREFIX + ownerId + "/"))
                .ifPresent(storage::delete);
    }

    /**
     * Reclaims pending scans older than {@code ttl} — the ones the user never
     * confirmed.
     *
     * @return how many objects were removed
     */
    public int purgeStalePending(Duration ttl) {
        Instant cutoff = Instant.now().minus(ttl);
        int removed = 0;
        for (String key : storage.keysModifiedBefore(PENDING_PREFIX, cutoff)) {
            if (storage.delete(key)) {
                removed++;
            }
        }
        if (removed > 0) {
            log.info("Purged {} abandoned scan(s) older than {}", removed, ttl);
        }
        return removed;
    }
}
