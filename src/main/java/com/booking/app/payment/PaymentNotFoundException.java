package com.booking.app.payment;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID id) {
        super("Payment with id %s not found".formatted(id));
    }
}
