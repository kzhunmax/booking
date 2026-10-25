package com.booking.app.resource.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating resource details")
public record UpdateResourceRequest(
        @Schema(description = "Updated name of the resource or specialist", example = "Master Barber - Alex")
        @NotBlank(message = "Name cannot be blank")
        @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
        String name,

        @Schema(description = "Updated description", example = "Senior master styling and haircut services")
        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description) {}
