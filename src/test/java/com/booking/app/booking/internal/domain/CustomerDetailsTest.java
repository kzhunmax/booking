package com.booking.app.booking.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CustomerDetailsTest {

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should create CustomerDetails with valid email and name")
        void shouldCreateCustomerDetailsWithValidEmailAndName() {
            CustomerDetails details = new CustomerDetails("user@example.com", "John Doe");

            assertThat(details.email()).isEqualTo("user@example.com");
            assertThat(details.name()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowWhenEmailIsNull() {
            assertThatThrownBy(() -> new CustomerDetails(null, "John Doe"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email cannot be blank");
        }

        @Test
        @DisplayName("Should throw exception when email is blank")
        void shouldThrowWhenEmailIsBlank() {
            assertThatThrownBy(() -> new CustomerDetails("   ", "John Doe"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email cannot be blank");
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowWhenNameIsNull() {
            assertThatThrownBy(() -> new CustomerDetails("user@example.com", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Name cannot be blank");
        }

        @Test
        @DisplayName("Should throw exception when name is blank")
        void shouldThrowWhenNameIsBlank() {
            assertThatThrownBy(() -> new CustomerDetails("user@example.com", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Name cannot be blank");
        }
    }
}
