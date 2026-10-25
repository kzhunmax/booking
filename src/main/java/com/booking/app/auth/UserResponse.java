package com.booking.app.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Details of a registered user profile")
public record UserResponse(
        @Schema(description = "Public unique identifier of the user", example = "c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a33")
        UUID publicId,

        @Schema(description = "User email address", example = "john.doe@example.com")
        String email,

        @Schema(description = "Full name of the user", example = "John Doe")
        String name,

        @Schema(description = "Assigned user role") UserRole role,

        @Schema(description = "User account status") UserStatus status,

        @Schema(description = "Account creation timestamp in UTC", example = "2026-10-01T12:00:00Z")
        Instant createdAt,

        @Schema(description = "Account last updated timestamp in UTC", example = "2026-10-01T12:00:00Z")
        Instant updatedAt) {}
