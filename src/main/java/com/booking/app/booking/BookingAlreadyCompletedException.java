package com.booking.app.booking;

public class BookingAlreadyCompletedException extends IllegalArgumentException {
    public BookingAlreadyCompletedException(String message) {
        super(message);
    }
}
