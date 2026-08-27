package com.booking.app.booking;

public class BookingAlreadyCompletedException extends InvalidStatusTransitionException {
    public BookingAlreadyCompletedException(String message) {
        super(message);
    }
}
