package com.dressd.avatar.api;

import com.dressd.avatar.api.dto.AvatarResponse;
import com.dressd.avatar.api.dto.SaveAvatarRequest;
import com.dressd.avatar.domain.BodyTypeCatalog;
import com.dressd.avatar.service.AvatarService;
import com.dressd.common.web.OwnerContext;
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
@RequestMapping("/api/avatars")
public class AvatarController {

    private final AvatarService service;

    public AvatarController(AvatarService service) {
        this.service = service;
    }

    /** The fixed body-type presets — no auth needed, this is static catalog data. */
    @GetMapping("/body-types")
    public List<BodyTypeCatalog.Preset> bodyTypes() {
        return BodyTypeCatalog.all();
    }

    @GetMapping
    public List<AvatarResponse> list(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner) {
        return service.list(OwnerId.resolve(owner)).stream().map(AvatarResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AvatarResponse get(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id) {
        return AvatarResponse.from(service.get(OwnerId.resolve(owner), id));
    }

    @PostMapping
    public ResponseEntity<AvatarResponse> create(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @Valid @RequestBody SaveAvatarRequest request) {
        AvatarResponse body = AvatarResponse.from(service.create(OwnerId.resolve(owner), request));
        return ResponseEntity.created(URI.create("/api/avatars/" + body.id())).body(body);
    }

    @PutMapping("/{id}")
    public AvatarResponse update(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id,
            @Valid @RequestBody SaveAvatarRequest request) {
        return AvatarResponse.from(service.update(OwnerId.resolve(owner), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = OwnerContext.OWNER_HEADER, required = false) String owner,
            @PathVariable UUID id) {
        service.delete(OwnerId.resolve(owner), id);
        return ResponseEntity.noContent().build();
    }
}
