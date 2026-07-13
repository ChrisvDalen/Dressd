package com.dressd.outfit.api;

import com.dressd.common.web.OwnerContext;
import com.dressd.outfit.api.dto.OutfitResponse;
import com.dressd.outfit.api.dto.SaveOutfitRequest;
import com.dressd.outfit.service.OutfitService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outfits")
public class OutfitController {

    private final OutfitService service;

    public OutfitController(OutfitService service) {
        this.service = service;
    }

    @GetMapping
    public List<OutfitResponse> list(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner) {
        return service.list(OwnerId.resolve(owner)).stream().map(OutfitResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OutfitResponse get(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id) {
        return OutfitResponse.from(service.get(OwnerId.resolve(owner), id));
    }

    @PostMapping
    public ResponseEntity<OutfitResponse> create(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @Valid @RequestBody SaveOutfitRequest request) {
        OutfitResponse body = OutfitResponse.from(service.create(OwnerId.resolve(owner), request));
        return ResponseEntity.created(URI.create("/api/outfits/" + body.id())).body(body);
    }

    @PutMapping("/{id}")
    public OutfitResponse update(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id,
            @Valid @RequestBody SaveOutfitRequest request) {
        return OutfitResponse.from(service.update(OwnerId.resolve(owner), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id) {
        service.delete(OwnerId.resolve(owner), id);
        return ResponseEntity.noContent().build();
    }
}
