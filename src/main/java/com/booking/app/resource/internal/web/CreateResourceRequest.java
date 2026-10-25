package com.booking.app.resource.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new resource or service")
public record CreateResourceRequest(
        @Schema(description = "Name of the resource or specialist", example = "Haircut & Beard Trim")
        @NotBlank(message = "Name cannot be blank")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        @Schema(
                description = "Detailed description of the resource",
                example = "Full grooming service with wash and styling")
        @Size(max = 10000, message = "Description cannot exceed 10000 characters")
        String description,

        @Schema(description = "Hourly rate or base service price", example = "50.00")
        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.01", message = "Price per hour must be greater than zero")
        BigDecimal pricePerHour,

        @Schema(description = "Three-letter ISO-4217 currency code", example = "USD")
        @NotBlank(message = "Currency cannot be blank")
        @Size(min = 3, max = 3, message = "Currency must be 3 characters long")
        String currency) {}
