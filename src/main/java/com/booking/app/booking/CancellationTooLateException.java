package com.booking.app.booking;

public class CancellationTooLateException extends RuntimeException {
    public CancellationTooLateException(String message) {
        super(message);
    }
}
