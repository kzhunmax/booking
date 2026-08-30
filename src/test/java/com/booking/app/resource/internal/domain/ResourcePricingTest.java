package com.booking.app.resource.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResourcePricingTest {

    @Test
    @DisplayName("Should create ResourcePricing with valid price and currency")
    void shouldCreateWithValidPriceAndCurrency() {
        ResourcePricing pricing = new ResourcePricing(BigDecimal.valueOf(100.50), "usd");

        assertThat(pricing.pricePerHour()).isEqualTo(BigDecimal.valueOf(100.50));
        assertThat(pricing.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should trim and uppercase currency code")
    void shouldNormalizeCurrencyCode() {
        ResourcePricing pricing = new ResourcePricing(BigDecimal.valueOf(50), "  eur  ");

        assertThat(pricing.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThatThrownBy(() -> new ResourcePricing(BigDecimal.valueOf(100), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when currency is blank")
    void shouldThrowWhenCurrencyIsBlank() {
        assertThatThrownBy(() -> new ResourcePricing(BigDecimal.valueOf(100), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency cannot be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"US", "USDD", "U", "1234"})
    @DisplayName("Should throw exception when currency code is not 3 characters long")
    void shouldThrowWhenCurrencyLengthInvalid(String currency) {
        assertThatThrownBy(() -> new ResourcePricing(BigDecimal.valueOf(100), currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency must be 3 characters long");
    }

    @Test
    @DisplayName("Should throw exception when pricePerHour is null")
    void shouldThrowWhenPricePerHourIsNull() {
        assertThatThrownBy(() -> new ResourcePricing(null, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price per hour cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when pricePerHour is zero or negative")
    void shouldThrowWhenPricePerHourIsNotPositive() {
        assertThatThrownBy(() -> new ResourcePricing(BigDecimal.ZERO, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price per hour must be greater than zero");

        assertThatThrownBy(() -> new ResourcePricing(BigDecimal.valueOf(-10), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price per hour must be greater than zero");
    }
}
