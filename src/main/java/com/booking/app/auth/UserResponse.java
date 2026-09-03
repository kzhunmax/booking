package com.booking.app.auth;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID publicId,
        String email,
        String name,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {}
