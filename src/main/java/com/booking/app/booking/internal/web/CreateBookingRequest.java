package com.booking.app.booking.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request payload for reserving a resource appointment")
public record CreateBookingRequest(
        @Schema(
                description = "ID of the target resource or specialist",
                example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        @NotNull(message = "resourceId is required")
        UUID resourceId,

        @Schema(description = "Requested start time in UTC", example = "2026-11-01T10:00:00Z")
        @NotNull(message = "startsAt is required")
        Instant startsAt,

        @Schema(description = "Requested end time in UTC", example = "2026-11-01T11:00:00Z")
        @NotNull(message = "endsAt is required")
        Instant endsAt) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isEndsAtAfterStartsAt() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
