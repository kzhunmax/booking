package com.booking.app.payment.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.booking.app.payment.PaymentResult;
import com.stripe.StripeClient;
import com.stripe.exception.ApiException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.service.PaymentIntentService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StripePaymentGatewayTest {

    @Test
    @DisplayName("Mock mode: charge below threshold succeeds with pi_mock reference")
    void shouldChargeSuccessfullyInMockModeBelowThreshold() {
        StripePaymentGateway gateway = new StripePaymentGateway("sk_test_mock");

        PaymentResult result = gateway.charge(BigDecimal.valueOf(100.0), "USD");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.gatewayReference()).startsWith("pi_mock_");
    }

    @Test
    @DisplayName("Mock mode: charge above threshold fails")
    void shouldFailToChargeInMockModeAboveThreshold() {
        StripePaymentGateway gateway = new StripePaymentGateway("sk_test_mock");

        PaymentResult result = gateway.charge(BigDecimal.valueOf(5000.01), "USD");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.gatewayReference()).isEqualTo("stripe_failed_card_declined");
    }

    @Test
    @DisplayName("Client mode: payment intent succeeded returns success and intent id")
    void shouldReturnSuccessForSucceededPaymentIntentInClientMode() throws Exception {
        StripeClient client = mock(StripeClient.class, Mockito.RETURNS_DEEP_STUBS);
        PaymentIntentService service = mock(PaymentIntentService.class);
        PaymentIntent intent = mock(PaymentIntent.class);

        when(client.v1().paymentIntents()).thenReturn(service);
        when(service.create(any(PaymentIntentCreateParams.class))).thenReturn(intent);
        when(intent.getStatus()).thenReturn("succeeded");
        when(intent.getId()).thenReturn("pi_real_12345");

        StripePaymentGateway gateway = new StripePaymentGateway(client);

        PaymentResult result = gateway.charge(BigDecimal.valueOf(150.0), "usd");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.gatewayReference()).isEqualTo("pi_real_12345");
    }

    @Test
    @DisplayName("Client mode: payment intent not succeeded returns false")
    void shouldReturnFailureForUnsuccessfulPaymentIntentInClientMode() throws Exception {
        StripeClient client = mock(StripeClient.class, Mockito.RETURNS_DEEP_STUBS);
        PaymentIntentService service = mock(PaymentIntentService.class);
        PaymentIntent intent = mock(PaymentIntent.class);

        when(client.v1().paymentIntents()).thenReturn(service);
        when(service.create(any(PaymentIntentCreateParams.class))).thenReturn(intent);
        when(intent.getStatus()).thenReturn("requires_payment_method");
        when(intent.getId()).thenReturn("pi_failed_12345");

        StripePaymentGateway gateway = new StripePaymentGateway(client);

        PaymentResult result = gateway.charge(BigDecimal.valueOf(150.0), "usd");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.gatewayReference()).isEqualTo("pi_failed_12345");
    }

    @Test
    @DisplayName("Client mode: StripeException is caught and handled gracefully")
    void shouldHandleStripeExceptionInClientMode() throws Exception {
        StripeClient client = mock(StripeClient.class, Mockito.RETURNS_DEEP_STUBS);
        PaymentIntentService service = mock(PaymentIntentService.class);

        when(client.v1().paymentIntents()).thenReturn(service);
        when(service.create(any(PaymentIntentCreateParams.class)))
                .thenThrow(new ApiException("Card error", "req_1", "card_declined", 402, null));

        StripePaymentGateway gateway = new StripePaymentGateway(client);

        PaymentResult result = gateway.charge(BigDecimal.valueOf(150.0), "usd");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.gatewayReference()).isEqualTo("stripe_failed_card_declined");
    }
}
