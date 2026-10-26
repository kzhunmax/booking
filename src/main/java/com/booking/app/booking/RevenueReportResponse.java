package com.booking.app.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Revenue report for bookings with a given status in a time range")
public record RevenueReportResponse(
        @Schema(description = "Start of the range (booking start time, inclusive)", example = "2026-11-01T00:00:00Z")
        Instant from,

        @Schema(description = "End of the range (booking start time, inclusive)", example = "2026-11-30T23:59:59Z")
        Instant to,

        @Schema(description = "Booking status taken into account", example = "CONFIRMED")
        BookingStatus status,

        @Schema(description = "Total revenue in the range", example = "4500.00")
        BigDecimal totalRevenue,

        @Schema(description = "Revenue per resource, highest first")
        List<ResourceRevenueResponse> resources) {}
