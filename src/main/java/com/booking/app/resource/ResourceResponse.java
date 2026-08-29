package com.booking.app.resource;

import java.math.BigDecimal;
import java.util.UUID;

public record ResourceResponse(
        UUID publicId,
        String name,
        String description,
        ResourceStatus status,
        BigDecimal pricePerHour,
        String currency) {}
