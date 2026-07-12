package com.dressd.wardrobe.api;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.common.domain.Season;
import com.dressd.common.web.OwnerContext;
import com.dressd.wardrobe.api.dto.CreateGarmentRequest;
import com.dressd.wardrobe.api.dto.GarmentResponse;
import com.dressd.wardrobe.api.dto.UpdateGarmentRequest;
import com.dressd.wardrobe.service.GarmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/garments")
public class GarmentController {

    private final GarmentService service;

    public GarmentController(GarmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<GarmentResponse> list(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @RequestParam(required = false) GarmentCategory category,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Season season) {
        UUID ownerId = OwnerId.resolve(owner);
        return service.search(ownerId, category, color, season).stream()
                .map(GarmentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public GarmentResponse get(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id) {
        return GarmentResponse.from(service.get(OwnerId.resolve(owner), id));
    }

    @PostMapping
    public ResponseEntity<GarmentResponse> create(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @Valid @RequestBody CreateGarmentRequest request) {
        GarmentResponse body = GarmentResponse.from(service.create(OwnerId.resolve(owner), request));
        return ResponseEntity.created(URI.create("/api/garments/" + body.id())).body(body);
    }

    @PatchMapping("/{id}")
    public GarmentResponse update(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id,
            @RequestBody UpdateGarmentRequest request) {
        return GarmentResponse.from(service.update(OwnerId.resolve(owner), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id) {
        service.delete(OwnerId.resolve(owner), id);
        return ResponseEntity.noContent().build();
    }
}
