package com.booking.app.payment.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booking.app.payment.InvalidStatusTransitionException;
import com.booking.app.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentTest {

    private UUID bookingId;
    private UUID userId;
    private UUID idempotencyKey;
    private BigDecimal amount;
    private String currency;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
        amount = BigDecimal.valueOf(100.00);
        currency = "USD";
    }

    private Payment createValidPayment() {
        return new Payment(bookingId, userId, amount, currency, idempotencyKey);
    }

    @Nested
    @DisplayName("Payment Creation")
    class Creation {

        @Test
        @DisplayName("Should create payment with valid fields in PENDING status")
        void shouldCreatePaymentWithValidFields() {
            Payment payment = createValidPayment();

            assertThat(payment.getPublicId()).isNotNull();
            assertThat(payment.getBookingId()).isEqualTo(bookingId);
            assertThat(payment.getUserId()).isEqualTo(userId);
            assertThat(payment.getAmount()).isEqualTo(amount);
            assertThat(payment.getCurrency()).isEqualTo("USD");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
            assertThat(payment.getGatewayReference()).isNull();
            assertThat(payment.getAuditInfo()).isNotNull();

            // Hibernate generates
            assertThat(payment.getVersion()).isNull();
        }

        @Test
        @DisplayName("Should trim and uppercase currency code")
        void shouldNormalizeCurrency() {
            Payment payment = new Payment(bookingId, userId, amount, "  eur  ", idempotencyKey);
            assertThat(payment.getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Should throw exception when bookingId is null")
        void shouldThrowExceptionWhenBookingIdIsNull() {
            assertThatThrownBy(() -> new Payment(null, userId, amount, currency, idempotencyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("bookingId cannot be null");
        }

        @Test
        @DisplayName("Should allow null userId")
        void shouldAllowNullUserId() {
            Payment payment = new Payment(bookingId, null, amount, currency, idempotencyKey);
            assertThat(payment.getUserId()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> new Payment(bookingId, userId, null, currency, idempotencyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("amount cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when amount is zero or negative")
        void shouldThrowExceptionWhenAmountIsNotPositive() {
            assertThatThrownBy(() -> new Payment(bookingId, userId, BigDecimal.ZERO, currency, idempotencyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("amount must be greater than zero");

            assertThatThrownBy(() -> new Payment(bookingId, userId, BigDecimal.valueOf(-10), currency, idempotencyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("amount must be greater than zero");
        }

        @Test
        @DisplayName("Should throw exception when currency is null or blank")
        void shouldThrowExceptionWhenCurrencyIsNullOrBlank() {
            assertThatThrownBy(() -> new Payment(bookingId, userId, amount, null, idempotencyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currency cannot be null");

            assertThatThrownBy(() -> new Payment(bookingId, userId, amount, "   ", idempotencyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currency must be 3 characters long");
        }

        @ParameterizedTest
        @ValueSource(strings = {"US", "USDD", "U", "1234"})
        @DisplayName("Should throw exception when currency code length != 3")
        void shouldThrowExceptionWhenCurrencyLengthInvalid(String invalidCurrency) {
            assertThatThrownBy(() -> new Payment(bookingId, userId, amount, invalidCurrency, idempotencyKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currency must be 3 characters long");
        }

        @Test
        @DisplayName("Should throw exception when idempotencyKey is null")
        void shouldThrowExceptionWhenIdempotencyKeyIsNull() {
            assertThatThrownBy(() -> new Payment(bookingId, userId, amount, currency, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("idempotencyKey cannot be null");
        }
    }

    @Nested
    @DisplayName("Status Transitions (PENDING -> SUCCEEDED / FAILED / REFUNDED)")
    class StatusTransitions {

        @Test
        @DisplayName("Should mark payment as SUCCEEDED with gateway reference")
        void shouldMarkAsSucceeded() {
            Payment payment = createValidPayment();
            payment.markAsSucceeded("gw_ref_123");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(payment.getGatewayReference()).isEqualTo("gw_ref_123");
        }

        @Test
        @DisplayName("Should be idempotent when marking as SUCCEEDED multiple times")
        void shouldBeIdempotentWhenMarkingAsSucceeded() {
            Payment payment = createValidPayment();
            payment.markAsSucceeded("gw_ref_123");
            payment.markAsSucceeded("gw_ref_123");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        }

        @Test
        @DisplayName("Should throw exception when markAsSucceeded has null gateway reference")
        void shouldThrowWhenMarkAsSucceededGatewayRefIsNull() {
            Payment payment = createValidPayment();

            assertThatThrownBy(() -> payment.markAsSucceeded(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("gatewayReference cannot be null");
        }

        @Test
        @DisplayName("Should mark payment as FAILED with gateway reference")
        void shouldMarkAsFailed() {
            Payment payment = createValidPayment();
            payment.markAsFailed("gw_fail_123");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getGatewayReference()).isEqualTo("gw_fail_123");
        }

        @Test
        @DisplayName("Should be idempotent when marking as FAILED multiple times")
        void shouldBeIdempotentWhenMarkingAsFailed() {
            Payment payment = createValidPayment();
            payment.markAsFailed("gw_fail_123");
            payment.markAsFailed("gw_fail_123");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Should not allow markAsSucceeded on FAILED payment")
        void shouldNotAllowSucceedOnFailedPayment() {
            Payment payment = createValidPayment();
            payment.markAsFailed("fail_ref");

            assertThatThrownBy(() -> payment.markAsSucceeded("new_ref"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot succeed a payment with status FAILED");
        }

        @Test
        @DisplayName("Should not allow markAsFailed on SUCCEEDED payment")
        void shouldNotAllowFailOnSucceededPayment() {
            Payment payment = createValidPayment();
            payment.markAsSucceeded("success_ref");

            assertThatThrownBy(() -> payment.markAsFailed("fail_ref"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot fail a payment with status SUCCEEDED");
        }

        @Test
        @DisplayName("Should refund SUCCEEDED payment")
        void shouldRefundSucceededPayment() {
            Payment payment = createValidPayment();
            payment.markAsSucceeded("success_ref");

            payment.refund();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Should be idempotent when refunding already REFUNDED payment")
        void shouldBeIdempotentWhenRefunding() {
            Payment payment = createValidPayment();
            payment.markAsSucceeded("success_ref");
            payment.refund();

            payment.refund();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Should not refund PENDING payment")
        void shouldNotRefundPendingPayment() {
            Payment payment = createValidPayment();

            assertThatThrownBy(payment::refund)
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Only SUCCEEDED payments can be refunded");
        }

        @Test
        @DisplayName("Should not refund FAILED payment")
        void shouldNotRefundFailedPayment() {
            Payment payment = createValidPayment();
            payment.markAsFailed("fail_ref");

            assertThatThrownBy(payment::refund)
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Only SUCCEEDED payments can be refunded");
        }

        @Test
        @DisplayName("Should not allow markAsSucceeded on REFUNDED payment")
        void shouldNotAllowSucceedOnRefundedPayment() {
            Payment payment = createValidPayment();
            payment.markAsSucceeded("success_ref");
            payment.refund();

            assertThatThrownBy(() -> payment.markAsSucceeded("new_ref"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot succeed a payment with status REFUNDED");
        }

        @Test
        @DisplayName("Should not allow markAsFailed on REFUNDED payment")
        void shouldNotAllowFailOnRefundedPayment() {
            Payment payment = createValidPayment();
            payment.markAsSucceeded("success_ref");
            payment.refund();

            assertThatThrownBy(() -> payment.markAsFailed("fail_ref"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot fail a payment with status REFUNDED");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class Equality {

        @Test
        @DisplayName("Should verify equals and hashCode contract")
        void shouldVerifyEqualsAndHashCode() {
            EqualsVerifier.forClass(Payment.class)
                    .withOnlyTheseFields("publicId")
                    .suppress(Warning.NONFINAL_FIELDS)
                    .verify();
        }
    }
}
