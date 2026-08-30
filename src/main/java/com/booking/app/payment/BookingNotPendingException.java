package com.booking.app.payment;

import com.booking.app.booking.BookingStatus;
import java.util.UUID;

public class BookingNotPendingException extends RuntimeException {
    public BookingNotPendingException(UUID bookingId, BookingStatus status) {
        super("Booking %s status is %s, expected PENDING".formatted(bookingId, status));
    }
}
