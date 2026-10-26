package com.booking.app.payment.internal.infrastructure;

import com.booking.app.payment.PaymentGateway;
import com.booking.app.payment.PaymentResult;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);
    private static final BigDecimal MOCK_THRESHOLD = BigDecimal.valueOf(5000);

    private final StripeClient stripeClient;

    @Autowired
    public StripePaymentGateway(@Value("${app.stripe.secret-key}") String apiKey) {
        this.stripeClient = (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("sk_test_mock"))
                ? new StripeClient(apiKey)
                : null;
    }

    StripePaymentGateway(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    @Override
    public PaymentResult charge(BigDecimal amount, String currency) {
        log.info("Processing payment via Stripe: amount={} currency={}", amount, currency);

        if (stripeClient == null) {
            log.info("Stripe running in sandbox/mock mode (mock API key detected)");
            if (amount.compareTo(MOCK_THRESHOLD) <= 0) {
                return new PaymentResult(true, "pi_mock_" + UUID.randomUUID());
            }
            return new PaymentResult(false, "stripe_failed_card_declined");
        }

        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase(Locale.ROOT))
                    .setPaymentMethod("pm_card_visa")
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build())
                    .setDescription("Service booking payment")
                    .build();

            PaymentIntent paymentIntent = stripeClient.v1().paymentIntents().create(params);
            log.info(
                    "Stripe PaymentIntent processed: id={}, status={}",
                    paymentIntent.getId(),
                    paymentIntent.getStatus());

            if ("succeeded".equalsIgnoreCase(paymentIntent.getStatus())) {
                return new PaymentResult(true, paymentIntent.getId());
            } else {
                log.warn("Stripe PaymentIntent status not succeeded: status={}", paymentIntent.getStatus());
                return new PaymentResult(false, paymentIntent.getId());
            }
        } catch (StripeException e) {
            log.error("Stripe API communication failed: code={}, message={}", e.getCode(), e.getMessage(), e);
            return new PaymentResult(false, "stripe_failed_" + (e.getCode() != null ? e.getCode() : "api_error"));
        } catch (Exception e) {
            log.error("Unexpected error during Stripe charge", e);
            return new PaymentResult(false, "stripe_failed_unexpected");
        }
    }
}
