package com.booking.app.payment.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request payload for initiating a payment for a booking")
public record CreatePaymentRequest(
        @Schema(description = "ID of the booking to pay for", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        @NotNull(message = "bookingId is required")
        UUID bookingId,

        @Schema(
                description = "User ID (required if an administrator performs the payment on behalf of a user)",
                example = "c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a33")
        @NotNull(message = "userId is required")
        UUID userId) {}
