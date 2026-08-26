package com.booking.app.booking.internal.domain;

import java.time.Instant;

public record BookingInterval(Instant startsAt, Instant endsAt) {
    public BookingInterval {
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("Interval values cannot be null");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("ends_at must be after starts_at");
        }
    }
}
