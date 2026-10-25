package com.booking.app.resource.internal.web;

import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceService;
import com.booking.app.resource.ResourceStatus;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ResourceController implements ResourceApi {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public ResponseEntity<ResourceResponse> getById(UUID publicId) {
        return ResponseEntity.ok(resourceService.findByPublicId(publicId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> createResource(CreateResourceRequest request) {
        ResourceResponse created = resourceService.createResource(
                request.name(), request.description(), request.pricePerHour(), request.currency());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.publicId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    public Page<ResourceResponse> getResources(
            ResourceStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        ResourceStatus filterStatus = (status != null) ? status : ResourceStatus.ACTIVE;
        return resourceService.findAll(filterStatus, pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> update(UUID publicId, UpdateResourceRequest request) {
        ResourceResponse updated = resourceService.update(publicId, request.name(), request.description());
        return ResponseEntity.ok(updated);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> updateStatus(UUID publicId, UpdateStatusRequest request) {
        ResourceResponse updated = resourceService.updateStatus(publicId, request.status());
        return ResponseEntity.ok(updated);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archive(UUID publicId) {
        resourceService.archive(publicId);
        return ResponseEntity.noContent().build();
    }
}
