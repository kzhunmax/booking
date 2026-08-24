package com.booking.app.resource.internal.api;

import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceService;
import com.booking.app.resource.ResourceStatus;
import com.booking.app.resource.internal.dto.CreateResourceRequest;
import com.booking.app.resource.internal.dto.UpdateResourceRequest;
import com.booking.app.resource.internal.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ResourceResponse> getById(@PathVariable UUID publicId) {
        return ResponseEntity.ok(resourceService.findByPublicId(publicId));
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest request) {
        ResourceResponse created = resourceService.createResource(request.name(), request.description());
        URI location = URI.create("/api/resources/" + created.publicId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<ResourceResponse> getResources(
            @RequestParam(required = false) ResourceStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        ResourceStatus filterStatus = (status != null) ? status : ResourceStatus.ACTIVE;
        return resourceService.findAll(filterStatus, pageable);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable UUID publicId, @Valid @RequestBody UpdateResourceRequest request) {
        ResourceResponse updated = resourceService.update(publicId, request.name(), request.description());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{publicId}/status")
    public ResponseEntity<ResourceResponse> updateStatus(
            @PathVariable UUID publicId, @Valid @RequestBody UpdateStatusRequest request) {
        ResourceResponse updated = resourceService.updateStatus(publicId, request.status());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> archive(@PathVariable UUID publicId) {
        resourceService.archive(publicId);
        return ResponseEntity.noContent().build();
    }
}
