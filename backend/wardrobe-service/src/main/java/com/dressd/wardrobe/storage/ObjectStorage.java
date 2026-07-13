package com.dressd.wardrobe.storage;

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
}
