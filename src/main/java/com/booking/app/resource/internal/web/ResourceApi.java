package com.booking.app.resource.internal.web;

import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@Tag(name = "Resources", description = "Endpoints for managing bookable resources, specialists, and services")
@RequestMapping("/api/resources")
public interface ResourceApi {

    @Operation(
            summary = "Get resource by ID",
            description = "Fetches complete details for a single resource by its public UUID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resource found successfully"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @GetMapping("/{publicId}")
    ResponseEntity<ResourceResponse> getById(
            @Parameter(description = "Public UUID of the resource", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
                    @PathVariable
                    UUID publicId);

    @Operation(
            summary = "Create a new resource",
            description = "Creates a new bookable resource/service. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Resource created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "409", description = "Resource name already taken")
    })
    @PostMapping
    ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest request);

    @Operation(
            summary = "List all resources",
            description = "Retrieves a paginated list of resources, optionally filtered by status.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Resources retrieved successfully")})
    @GetMapping
    Page<ResourceResponse> getResources(
            @Parameter(description = "Filter by resource status (defaults to ACTIVE)") @RequestParam(required = false)
                    ResourceStatus status,
            Pageable pageable);

    @Operation(
            summary = "Update resource details",
            description = "Updates the name and description of an existing resource. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resource updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PutMapping("/{publicId}")
    ResponseEntity<ResourceResponse> update(
            @Parameter(description = "Public UUID of the resource") @PathVariable UUID publicId,
            @Valid @RequestBody UpdateResourceRequest request);

    @Operation(
            summary = "Update resource status",
            description = "Changes operational status (ACTIVE, INACTIVE, ARCHIVED). Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PatchMapping("/{publicId}/status")
    ResponseEntity<ResourceResponse> updateStatus(
            @Parameter(description = "Public UUID of the resource") @PathVariable UUID publicId,
            @Valid @RequestBody UpdateStatusRequest request);

    @Operation(
            summary = "Archive a resource",
            description = "Soft-deletes/archives the resource. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Resource archived successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @DeleteMapping("/{publicId}")
    ResponseEntity<Void> archive(@Parameter(description = "Public UUID of the resource") @PathVariable UUID publicId);
}
