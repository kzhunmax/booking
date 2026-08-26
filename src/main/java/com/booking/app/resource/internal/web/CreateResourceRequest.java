package com.booking.app.resource.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResourceRequest(
        @NotBlank(message = "Name cannot be blank") @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        @NotNull @Size(max = 10000, message = "Description cannot exceed 10000 characters")
        String description) {}
