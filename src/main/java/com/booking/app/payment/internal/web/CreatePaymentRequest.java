package com.booking.app.payment.internal.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull(message = "bookingId is required") UUID bookingId,

        @NotNull(message = "userId is required") UUID userId) {}
