package com.booking.app.resource.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateResourceRequest(
        @NotBlank(message = "Name cannot be blank") @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        @Size(max = 10000, message = "Description cannot exceed 10000 characters")
        String description,

        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.01", message = "Price per hour must be greater than zero")
        BigDecimal pricePerHour,

        @NotBlank(message = "Currency cannot be blank")
        @Size(min = 3, max = 3, message = "Currency must be 3 characters long")
        String currency) {}
