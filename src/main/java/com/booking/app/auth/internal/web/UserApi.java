package com.booking.app.auth.internal.web;

import com.booking.app.auth.UserResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Users", description = "Endpoints for administrative user account management")
@RequestMapping("/api/users")
public interface UserApi {

    @Operation(
            summary = "List all registered users",
            description = "Retrieves a paginated list of all users. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users page returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping
    Page<UserResponse> findAllUsers(Pageable pageable);

    @Operation(
            summary = "Update user account status",
            description = "Updates the account status (ACTIVE, SUSPENDED) of a user. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{publicId}/status")
    ResponseEntity<UserResponse> updateStatus(
            @Parameter(description = "Public UUID of the user", example = "c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a33")
                    @PathVariable
                    UUID publicId,
            @Valid @RequestBody UpdateUserStatusRequest request);
}
