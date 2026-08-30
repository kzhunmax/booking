package com.booking.app.payment.internal.infrastructure;

import com.booking.app.payment.PaymentResponse;
import com.booking.app.payment.internal.domain.Payment;

public final class PaymentMapper {
    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPublicId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getGatewayReference());
    }
}
