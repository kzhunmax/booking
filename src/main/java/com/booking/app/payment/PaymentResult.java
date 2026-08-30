package com.booking.app.payment;

public record PaymentResult(boolean isSuccess, String gatewayReference) {}
