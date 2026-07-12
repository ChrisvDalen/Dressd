package com.dressd.wardrobe.api;

import com.dressd.common.web.OwnerContext;
import com.dressd.wardrobe.api.dto.ScanResultResponse;
import com.dressd.wardrobe.service.ScanService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Scan endpoint (SPEC.md flow A). Accepts a photo, returns a cut-out preview
 * plus suggested metadata. The client then confirms via POST /api/garments.
 */
@RestController
@RequestMapping("/api/scan")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ScanResultResponse scan(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        UUID ownerId = OwnerId.resolve(owner);
        try {
            return scanService.scan(ownerId, file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
