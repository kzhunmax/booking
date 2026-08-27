package com.booking.app.booking;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID publicId,
        UUID resourceId,
        String customerEmail,
        String customerName,
        Instant startsAt,
        Instant endsAt,
        BookingStatus status) {}
