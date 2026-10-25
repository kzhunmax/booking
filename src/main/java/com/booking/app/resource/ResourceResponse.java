package com.booking.app.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Details of a bookable resource or service")
public record ResourceResponse(
        @Schema(description = "Public unique identifier", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID publicId,

        @Schema(description = "Name of the service or specialist", example = "Haircut & Beard Trim")
        String name,

        @Schema(
                description = "Detailed description of the service",
                example = "Full grooming service with wash and styling")
        String description,

        @Schema(description = "Current availability status of the resource")
        ResourceStatus status,

        @Schema(description = "Price per hour or base visit fee", example = "50.00")
        BigDecimal pricePerHour,

        @Schema(description = "ISO-4217 three-letter currency code", example = "USD")
        String currency) {}
