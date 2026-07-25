package com.dressd.outfit.api;

import com.dressd.common.web.CurrentOwner;
import com.dressd.common.web.PageRequests;
import com.dressd.common.web.PageResponse;
import com.dressd.outfit.api.dto.OutfitResponse;
import com.dressd.outfit.api.dto.SaveOutfitRequest;
import com.dressd.outfit.service.OutfitService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outfits")
public class OutfitController {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "updatedAt");

    private final OutfitService service;

    public OutfitController(OutfitService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<OutfitResponse> list(
            @CurrentOwner UUID ownerId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.of(
                service.list(ownerId, PageRequests.of(page, size, NEWEST_FIRST)),
                OutfitResponse::from);
    }

    @GetMapping("/{id}")
    public OutfitResponse get(@CurrentOwner UUID ownerId, @PathVariable UUID id) {
        return OutfitResponse.from(service.get(ownerId, id));
    }

    @PostMapping
    public ResponseEntity<OutfitResponse> create(
            @CurrentOwner UUID ownerId,
            @Valid @RequestBody SaveOutfitRequest request) {
        OutfitResponse body = OutfitResponse.from(service.create(ownerId, request));
        return ResponseEntity.created(URI.create("/api/outfits/" + body.id())).body(body);
    }

    @PutMapping("/{id}")
    public OutfitResponse update(
            @CurrentOwner UUID ownerId,
            @PathVariable UUID id,
            @Valid @RequestBody SaveOutfitRequest request) {
        return OutfitResponse.from(service.update(ownerId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentOwner UUID ownerId, @PathVariable UUID id) {
        service.delete(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
