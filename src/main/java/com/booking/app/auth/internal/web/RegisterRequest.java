package com.booking.app.auth.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for user registration")
public record RegisterRequest(
        @Schema(description = "User email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Full name of the user", example = "John Doe")
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be maximum 255 characters long")
        String name,

        @Schema(
                description = "Password (min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special character)",
                example = "SecurePass123!")
        @NotBlank(message = "Password is required")
        @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
        @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit")
        @Pattern(regexp = ".*[+@$!%*?&_#^\\-].*", message = "Password must contain at least one special character")
        @Pattern(regexp = "^[A-Za-z\\d+@$!%*?&_#^\\-]{8,}$", message = "Password contains invalid characters")
        @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
        String password) {}
