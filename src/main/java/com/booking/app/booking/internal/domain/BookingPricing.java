package com.booking.app.booking.internal.domain;

import com.booking.app.common.Require;
import java.math.BigDecimal;
import java.util.Locale;

public record BookingPricing(BigDecimal totalAmount, String currency) {

    private static final int CURRENCY_CODE_LENGTH = 3;

    public BookingPricing {
        Require.notNull(currency, "Currency cannot be null");
        currency = currency.strip().toUpperCase(Locale.ROOT);
        Require.argument(!currency.isBlank(), "Currency cannot be blank");
        Require.argument(currency.length() == CURRENCY_CODE_LENGTH, "currency must be 3 characters long");
        Require.notNull(totalAmount, "total amount cannot be null");
        Require.argument(totalAmount.compareTo(BigDecimal.ZERO) > 0, "total amount must be greater than zero");
    }
}
