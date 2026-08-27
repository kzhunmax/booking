package com.booking.app.resource.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResourceRequest(
        @NotBlank(message = "Name cannot be blank")
        @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
        String name,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description) {}
