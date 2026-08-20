package com.dressd.wardrobe.api;

import com.dressd.common.web.BadRequestException;
import com.dressd.common.web.CurrentOwner;
import com.dressd.wardrobe.api.dto.ScanResultResponse;
import com.dressd.wardrobe.service.ScanService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
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

    private static final List<String> ACCEPTED_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp", "image/heic");

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ScanResultResponse scan(@CurrentOwner UUID ownerId, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image type: " + contentType);
        }
        try {
            return scanService.scan(ownerId, file.getBytes(),
                    StringUtils.getFilename(file.getOriginalFilename()), contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
