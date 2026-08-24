package com.booking.app.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
        @NotBlank(message = "Name must not be empty") @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        @Size(max = 10000, message = "Description cannot exceed 10000 characters")
        String description) {}
