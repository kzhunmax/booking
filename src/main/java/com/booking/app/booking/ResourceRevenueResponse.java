package com.booking.app.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Booking count and revenue for a single resource")
public record ResourceRevenueResponse(
        @Schema(description = "ID of the resource", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID resourceId,

        @Schema(description = "Number of bookings counted", example = "12")
        long totalBookings,

        @Schema(description = "Sum of booking amounts", example = "3000.00")
        BigDecimal totalRevenue) {}
