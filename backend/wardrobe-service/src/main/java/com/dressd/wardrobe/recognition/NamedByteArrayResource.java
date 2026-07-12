package com.dressd.wardrobe.recognition;

import org.springframework.core.io.ByteArrayResource;

/**
 * ByteArrayResource that reports a filename, required for multipart file parts.
 */
class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    NamedByteArrayResource(byte[] byteArray, String filename) {
        super(byteArray);
        this.filename = filename == null ? "upload.jpg" : filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
