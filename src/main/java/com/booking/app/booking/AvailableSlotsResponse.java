package com.booking.app.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Available time slots for a given resource and date")
public record AvailableSlotsResponse(
        @Schema(description = "ID of the resource", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID resourceId,

        @Schema(description = "Date queried", example = "2026-11-01")
        LocalDate date,

        @Schema(description = "List of open, non-overlapping available intervals")
        List<TimeSlot> slots) {

    @Schema(description = "Continuous available time window")
    public record TimeSlot(
            @Schema(description = "Slot start time in UTC", example = "2026-11-01T09:00:00Z")
            Instant startsAt,

            @Schema(description = "Slot end time in UTC", example = "2026-11-01T10:00:00Z")
            Instant endsAt) {}
}
