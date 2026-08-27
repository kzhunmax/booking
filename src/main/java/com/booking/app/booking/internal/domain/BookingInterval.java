package com.booking.app.booking.internal.domain;

import java.time.Duration;
import java.time.Instant;

public record BookingInterval(Instant startsAt, Instant endsAt) {
    public BookingInterval {
        if (startsAt == null) {
            throw new IllegalArgumentException("startsAt cannot be null");
        }
        if (endsAt == null) {
            throw new IllegalArgumentException("endsAt cannot be null");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("ends_at must be after starts_at");
        }
    }

    public Duration duration() {
        return Duration.between(startsAt, endsAt);
    }

    public boolean overlaps(BookingInterval other) {
        if (other == null) {
            throw new IllegalArgumentException("other interval cannot be null");
        }
        return this.startsAt.isBefore(other.endsAt) && this.endsAt.isAfter(other.startsAt);
    }
}
