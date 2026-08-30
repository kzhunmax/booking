package com.booking.app.booking.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookingPricingTest {

    @Test
    @DisplayName("Should create BookingPricing with valid totalAmount and currency")
    void shouldCreateWithValidTotalAmountAndCurrency() {
        BookingPricing pricing = new BookingPricing(BigDecimal.valueOf(250.00), "usd");

        assertThat(pricing.totalAmount()).isEqualTo(BigDecimal.valueOf(250.00));
        assertThat(pricing.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should trim and uppercase currency code")
    void shouldNormalizeCurrencyCode() {
        BookingPricing pricing = new BookingPricing(BigDecimal.valueOf(50), "  eur  ");

        assertThat(pricing.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThatThrownBy(() -> new BookingPricing(BigDecimal.valueOf(100), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when currency is blank")
    void shouldThrowWhenCurrencyIsBlank() {
        assertThatThrownBy(() -> new BookingPricing(BigDecimal.valueOf(100), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency cannot be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"US", "USDD", "U", "1234"})
    @DisplayName("Should throw exception when currency code is not 3 characters long")
    void shouldThrowWhenCurrencyLengthInvalid(String currency) {
        assertThatThrownBy(() -> new BookingPricing(BigDecimal.valueOf(100), currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency must be 3 characters long");
    }

    @Test
    @DisplayName("Should throw exception when totalAmount is null")
    void shouldThrowWhenTotalAmountIsNull() {
        assertThatThrownBy(() -> new BookingPricing(null, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("total amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when totalAmount is zero or negative")
    void shouldThrowWhenTotalAmountIsNotPositive() {
        assertThatThrownBy(() -> new BookingPricing(BigDecimal.ZERO, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("total amount must be greater than zero");

        assertThatThrownBy(() -> new BookingPricing(BigDecimal.valueOf(-10), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("total amount must be greater than zero");
    }
}
