package com.booking.app.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Details of a confirmed or pending booking")
public record BookingResponse(
        @Schema(
                description = "Public unique identifier of the booking",
                example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        UUID publicId,

        @Schema(
                description = "Public unique identifier of the booked resource",
                example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID resourceId,

        @Schema(description = "Email of the customer who booked the service", example = "client@example.com")
        String customerEmail,

        @Schema(description = "Full name of the customer", example = "John Doe")
        String customerName,

        @Schema(description = "Scheduled appointment start time in UTC", example = "2026-11-01T10:00:00Z")
        Instant startsAt,

        @Schema(description = "Scheduled appointment end time in UTC", example = "2026-11-01T11:00:00Z")
        Instant endsAt,

        @Schema(description = "Lifecycle status of the reservation")
        BookingStatus status,

        @Schema(description = "Total reservation cost calculated from duration and rate", example = "50.00")
        BigDecimal totalAmount,

        @Schema(description = "ISO-4217 three-letter currency code", example = "USD")
        String currency) {}
