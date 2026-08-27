package com.booking.app.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailableSlotsResponse(UUID resourceId, LocalDate date, List<TimeSlot> slots) {

    public record TimeSlot(Instant startsAt, Instant endsAt) {}
}
