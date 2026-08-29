package com.booking.app.resource.internal.domain;

import com.booking.app.common.Require;
import java.math.BigDecimal;
import java.util.Locale;

public record ResourcePricing(BigDecimal pricePerHour, String currency) {

    private static final int CURRENCY_CODE_LENGTH = 3;

    public ResourcePricing {
        Require.notNull(currency, "Currency cannot be null");
        currency = currency.strip().toUpperCase(Locale.ROOT);
        Require.argument(!currency.isBlank(), "Currency cannot be blank");
        Require.argument(currency.length() == CURRENCY_CODE_LENGTH, "currency must be 3 characters long");
        Require.notNull(pricePerHour, "price per hour cannot be null");
        Require.argument(pricePerHour.compareTo(BigDecimal.ZERO) > 0, "price per hour must be greater than zero");
    }
}
