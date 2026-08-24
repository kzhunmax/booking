package com.booking.app.resource.internal.api;

import com.booking.app.resource.ResourceRequest;
import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceService;
import com.booking.app.resource.internal.domain.ResourceStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(resourceService.findByPublicId(id));
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody ResourceRequest request) {
        ResourceResponse created = resourceService.createResource(request);
        URI location = URI.create("/api/resources/" + created.publicId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<ResourceResponse> getResources(
            @RequestParam(required = false) ResourceStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return resourceService.search(status, pageable);
    }
}
