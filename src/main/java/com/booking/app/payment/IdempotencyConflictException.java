package com.booking.app.payment;

import java.util.UUID;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(UUID idempotencyKey, UUID existingBookingId) {
        super("Idempotency key '%s' was already used for different booking '%s'"
                .formatted(idempotencyKey, existingBookingId));
    }

    public IdempotencyConflictException(String message) {
        super(message);
    }

    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
