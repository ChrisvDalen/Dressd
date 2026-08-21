package com.dressd.avatar.api;

import com.dressd.avatar.api.dto.AvatarResponse;
import com.dressd.avatar.api.dto.SaveAvatarRequest;
import com.dressd.avatar.domain.BodyTypeCatalog;
import com.dressd.avatar.service.AvatarService;
import com.dressd.common.web.CurrentOwner;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/avatars")
public class AvatarController {

    private final AvatarService service;

    public AvatarController(AvatarService service) {
        this.service = service;
    }

    /** The fixed body-type presets — static catalog data, listed in dressd.auth.public-paths. */
    @GetMapping("/body-types")
    public List<BodyTypeCatalog.Preset> bodyTypes() {
        return BodyTypeCatalog.all();
    }

    /**
     * An owner holds at most one avatar per body type, so this list is bounded by
     * the catalog and needs no pagination.
     */
    @GetMapping
    public List<AvatarResponse> list(@CurrentOwner UUID ownerId) {
        return service.list(ownerId).stream().map(AvatarResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AvatarResponse get(@CurrentOwner UUID ownerId, @PathVariable("id") UUID id) {
        return AvatarResponse.from(service.get(ownerId, id));
    }

    @PostMapping
    public ResponseEntity<AvatarResponse> create(
            @CurrentOwner UUID ownerId,
            @Valid @RequestBody SaveAvatarRequest request) {
        AvatarResponse body = AvatarResponse.from(service.create(ownerId, request));
        return ResponseEntity.created(URI.create("/api/avatars/" + body.id())).body(body);
    }

    @PutMapping("/{id}")
    public AvatarResponse update(
            @CurrentOwner UUID ownerId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody SaveAvatarRequest request) {
        return AvatarResponse.from(service.update(ownerId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentOwner UUID ownerId, @PathVariable("id") UUID id) {
        service.delete(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
