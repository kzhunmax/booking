package com.booking.app.booking;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(UUID id) {
        super("Booking with id %s not found".formatted(id));
    }
}
