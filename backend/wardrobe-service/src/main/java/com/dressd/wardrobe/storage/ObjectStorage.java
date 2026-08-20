package com.dressd.wardrobe.storage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction over object storage for the cut-out PNGs (SPEC.md section 8 open
 * question: Azure Blob vs S3). v1 ships a local filesystem implementation; a
 * cloud implementation can be dropped in without touching callers.
 */
public interface ObjectStorage {

    /**
     * Store the given bytes and return a publicly resolvable URL/path.
     *
     * @param key         logical object key (e.g. {@code garments/<uuid>.png})
     * @param bytes       object content
     * @param contentType MIME type
     * @return URL the client can use to fetch the object
     */
    String store(String key, byte[] bytes, String contentType);

    /**
     * Move an object to a new key, e.g. promoting a confirmed scan out of the
     * pending area.
     *
     * @return the public URL of the object at its new key
     */
    String move(String sourceKey, String targetKey);

    /**
     * Delete the object at {@code key}.
     *
     * @return {@code true} if an object was actually removed
     */
    boolean delete(String key);

    /**
     * Recover the storage key from a URL previously returned by this storage.
     * Returns empty when the URL does not belong to this storage, which is how
     * callers reject client-supplied URLs pointing somewhere else.
     */
    Optional<String> keyForUrl(String url);

    /** Keys under {@code prefix} last modified before {@code cutoff}. */
    List<String> keysModifiedBefore(String prefix, Instant cutoff);
}
