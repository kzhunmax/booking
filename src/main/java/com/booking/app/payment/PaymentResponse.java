package com.booking.app.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Details of a processed payment attempt")
public record PaymentResponse(
        @Schema(
                description = "Public unique identifier of the payment",
                example = "d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a44")
        UUID publicId,

        @Schema(description = "Associated booking ID", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        UUID bookingId,

        @Schema(description = "User ID who executed the payment", example = "c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a33")
        UUID userId,

        @Schema(description = "Charged amount", example = "50.00")
        BigDecimal amount,

        @Schema(description = "Three-letter ISO-4217 currency code", example = "USD")
        String currency,

        @Schema(description = "Transaction status (PENDING, COMPLETED, FAILED)")
        PaymentStatus status,

        @Schema(
                description = "Idempotency key supplied with the request",
                example = "e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a55")
        UUID idempotencyKey,

        @Schema(description = "Payment gateway reference code", example = "fake_gw_8f5b8e99-4c11")
        String gatewayReference) {}
