package com.booking.app.auth.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for user authentication")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "User password", example = "SecurePass123!") @NotBlank(message = "Password is required")
        String password) {}
