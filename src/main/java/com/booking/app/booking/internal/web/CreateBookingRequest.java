package com.booking.app.booking.internal.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "resourceId is required") UUID resourceId,

        @NotNull(message = "startsAt is required") Instant startsAt,
        @NotNull(message = "endsAt is required") Instant endsAt) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isEndsAtAfterStartsAt() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
