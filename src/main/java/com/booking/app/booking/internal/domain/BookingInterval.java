package com.booking.app.booking.internal.domain;

import com.booking.app.common.Require;
import java.time.Duration;
import java.time.Instant;

public record BookingInterval(Instant startsAt, Instant endsAt) {
    public BookingInterval {
        Require.notNull(startsAt, "startsAt cannot be null");
        Require.notNull(endsAt, "endsAt cannot be null");
        Require.argument(endsAt.isAfter(startsAt), "endsAt must be after startsAt");
    }

    public Duration duration() {
        return Duration.between(startsAt, endsAt);
    }

    public boolean overlaps(BookingInterval other) {
        Require.notNull(other, "other interval cannot be null");
        return this.startsAt.isBefore(other.endsAt) && this.endsAt.isAfter(other.startsAt);
    }
}
