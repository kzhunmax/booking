package com.booking.app.booking;

import java.time.Instant;

public class BookingSlotAlreadyTakenException extends RuntimeException {
    public BookingSlotAlreadyTakenException(Instant startsAt, Instant endsAt, Throwable cause) {
        super("Booking slot interval from %s to %s already taken".formatted(startsAt, endsAt), cause);
    }
}
