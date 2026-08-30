package com.booking.app.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentResult charge(BigDecimal amount, String currency);
}
