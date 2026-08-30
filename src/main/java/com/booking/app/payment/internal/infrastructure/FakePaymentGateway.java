package com.booking.app.payment.internal.infrastructure;

import com.booking.app.payment.PaymentGateway;
import com.booking.app.payment.PaymentResult;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGateway implements PaymentGateway {

    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(5000);

    @Override
    public PaymentResult charge(BigDecimal amount, String currency) {
        if (amount.compareTo(THRESHOLD) <= 0) {
            return new PaymentResult(true, "fake_gw_" + UUID.randomUUID());
        }
        return new PaymentResult(false, "fake_failed_" + UUID.randomUUID());
    }
}
