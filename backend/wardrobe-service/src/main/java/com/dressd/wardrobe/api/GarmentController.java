package com.dressd.wardrobe.api;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import com.dressd.common.web.CurrentOwner;
import com.dressd.common.web.PageRequests;
import com.dressd.common.web.PageResponse;
import com.dressd.wardrobe.api.dto.CreateGarmentRequest;
import com.dressd.wardrobe.api.dto.GarmentResponse;
import com.dressd.wardrobe.api.dto.UpdateGarmentRequest;
import com.dressd.wardrobe.service.GarmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/garments")
public class GarmentController {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "updatedAt");

    private final GarmentService service;

    public GarmentController(GarmentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<GarmentResponse> list(
            @CurrentOwner UUID ownerId,
            @RequestParam(name = "category", required = false) GarmentCategory category,
            @RequestParam(name = "color", required = false) String color,
            @RequestParam(name = "season", required = false) Season season,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return PageResponse.of(
                service.search(ownerId, category, color, season, PageRequests.of(page, size, NEWEST_FIRST)),
                GarmentResponse::from);
    }

    @GetMapping("/{id}")
    public GarmentResponse get(@CurrentOwner UUID ownerId, @PathVariable("id") UUID id) {
        return GarmentResponse.from(service.get(ownerId, id));
    }

    @PostMapping
    public ResponseEntity<GarmentResponse> create(
            @CurrentOwner UUID ownerId,
            @Valid @RequestBody CreateGarmentRequest request) {
        GarmentResponse body = GarmentResponse.from(service.create(ownerId, request));
        return ResponseEntity.created(URI.create("/api/garments/" + body.id())).body(body);
    }

    @PatchMapping("/{id}")
    public GarmentResponse update(
            @CurrentOwner UUID ownerId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateGarmentRequest request) {
        return GarmentResponse.from(service.update(ownerId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentOwner UUID ownerId, @PathVariable("id") UUID id) {
        service.delete(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
