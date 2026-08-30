package com.booking.app.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID publicId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        UUID idempotencyKey,
        String gatewayReference) {}
