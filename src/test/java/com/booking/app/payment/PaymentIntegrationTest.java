package com.booking.app.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.auth.AuthResponse;
import com.booking.app.auth.internal.web.LoginRequest;
import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.internal.web.CreateBookingRequest;
import com.booking.app.payment.internal.domain.Payment;
import com.booking.app.payment.internal.infrastructure.PaymentRepository;
import com.booking.app.payment.internal.web.CreatePaymentRequest;
import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.internal.web.CreateResourceRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class PaymentIntegrationTest {

    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(50.0);
    private static final String DEFAULT_CURRENCY = "USD";

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private PaymentRepository paymentRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        AuthResponse auth = restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("admin@test.com", "Admin@P@ss1"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(auth).isNotNull();
        adminToken = auth.token();
    }

    private static ZonedDateTime tomorrowAt(int hour) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(2).atTime(hour, 0).atZone(ZoneOffset.UTC);
    }

    private static <T> List<T> runConcurrently(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<T> firstResult = executor.submit(gated(ready, start, first));
            Future<T> secondResult = executor.submit(gated(ready, start, second));
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Workers did not become ready");
            }
            start.countDown();
            return List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        }
    }

    private static <T> Callable<T> gated(CountDownLatch ready, CountDownLatch start, Callable<T> delegate) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for start signal");
            }
            return delegate.call();
        };
    }

    @Test
    @DisplayName("Create payment with unique Idempotency-Key -> 201 Created and confirms booking")
    void shouldCreatePaymentAndConfirmBooking() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(10);
        BookingResponse booking = createBooking(resource.publicId(), slot, slot.plusHours(2));
        UUID idempotencyKey = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PaymentResponse payment = restTestClient
                .post()
                .uri("/api/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePaymentRequest(booking.publicId(), userId))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(payment).isNotNull();
        assertThat(payment.bookingId()).isEqualTo(booking.publicId());
        assertThat(payment.userId()).isEqualTo(userId);
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.amount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(payment.currency()).isEqualTo(DEFAULT_CURRENCY);
        assertThat(payment.gatewayReference()).startsWith("fake_gw_");

        BookingResponse updatedBooking = restTestClient
                .get()
                .uri("/api/bookings/{publicId}", booking.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(BookingResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updatedBooking).isNotNull();
        assertThat(updatedBooking.status()).isEqualTo(BookingStatus.CONFIRMED);

        PaymentResponse fetched = restTestClient
                .get()
                .uri("/api/payments/{publicId}", payment.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.publicId()).isEqualTo(payment.publicId());
        assertThat(fetched.status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("GET /api/payments?bookingId= lists payment attempts for the booking")
    void shouldListPaymentAttemptsForBooking() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(11);
        BookingResponse booking = createBooking(resource.publicId(), slot, slot.plusHours(1));
        UUID userId = UUID.randomUUID();
        PaymentResponse created =
                postPayment(booking.publicId(), userId, UUID.randomUUID()).getResponseBody();
        assertThat(created).isNotNull();

        restTestClient
                .get()
                .uri("/api/payments?bookingId={bookingId}", booking.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content[0].publicId")
                .isEqualTo(created.publicId().toString())
                .jsonPath("$.content[0].bookingId")
                .isEqualTo(booking.publicId().toString())
                .jsonPath("$.totalElements")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/payments/{publicId} for a missing payment yields 404")
    void shouldReturnNotFoundWhenPaymentDoesNotExist() {
        restTestClient
                .get()
                .uri("/api/payments/{publicId}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Payment Not Found");
    }

    @Test
    @DisplayName("Repeated request with same Idempotency-Key -> 200 OK without re-processing")
    void shouldReturnOkOnRepeatedPaymentWithSameKey() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(14);
        BookingResponse booking = createBooking(resource.publicId(), slot, slot.plusHours(1));
        UUID idempotencyKey = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PaymentResponse first =
                postPayment(booking.publicId(), userId, idempotencyKey).getResponseBody();
        assertThat(first).isNotNull();

        PaymentResponse second = restTestClient
                .post()
                .uri("/api/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePaymentRequest(booking.publicId(), userId))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PaymentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(second).isNotNull();
        assertThat(second.publicId()).isEqualTo(first.publicId());
        assertThat(second.status()).isEqualTo(first.status());
    }

    @Test
    @DisplayName("Reusing Idempotency-Key for a different booking -> 422 Unprocessable Content")
    void shouldRejectKeyReusedForDifferentBooking() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot1 = tomorrowAt(10);
        ZonedDateTime slot2 = tomorrowAt(12);
        BookingResponse booking1 = createBooking(resource.publicId(), slot1, slot1.plusHours(1));
        BookingResponse booking2 = createBooking(resource.publicId(), slot2, slot2.plusHours(1));
        UUID sharedKey = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        postPayment(booking1.publicId(), userId, sharedKey);

        restTestClient
                .post()
                .uri("/api/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header("Idempotency-Key", sharedKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePaymentRequest(booking2.publicId(), userId))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Idempotency Conflict");
    }

    @Test
    @DisplayName("Paying for an already confirmed booking -> 409 Conflict")
    void shouldRejectPaymentForAlreadyConfirmedBooking() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(16);
        BookingResponse booking = createBooking(resource.publicId(), slot, slot.plusHours(1));
        UUID firstKey = UUID.randomUUID();
        UUID secondKey = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        postPayment(booking.publicId(), userId, firstKey);

        restTestClient
                .post()
                .uri("/api/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header("Idempotency-Key", secondKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePaymentRequest(booking.publicId(), userId))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Booking Not Pending");
    }

    @Test
    @DisplayName("Concurrent payments with the same Idempotency-Key yield one 201 and one 200")
    void shouldAllowOnlyOnePaymentCreatedConcurrentlyWithSameIdempotencyKey() throws Exception {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(18);
        BookingResponse booking = createBooking(resource.publicId(), slot, slot.plusHours(1));
        UUID sharedKey = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        List<EntityExchangeResult<PaymentResponse>> responses = runConcurrently(
                () -> postPayment(booking.publicId(), userId, sharedKey),
                () -> postPayment(booking.publicId(), userId, sharedKey));

        assertThat(responses.stream().map(EntityExchangeResult::getStatus).toList())
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.OK);

        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> p.getIdempotencyKey().equals(sharedKey))
                .toList();
        assertThat(payments).hasSize(1);
    }

    private EntityExchangeResult<PaymentResponse> postPayment(UUID bookingId, UUID userId, UUID idempotencyKey) {
        return restTestClient
                .post()
                .uri("/api/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePaymentRequest(bookingId, userId))
                .exchange()
                .expectBody(PaymentResponse.class)
                .returnResult();
    }

    private ResourceResponse createResource() {
        ResourceResponse body = restTestClient
                .post()
                .uri("/api/resources")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateResourceRequest(
                        "room-" + UUID.randomUUID(), "integration room", DEFAULT_PRICE, DEFAULT_CURRENCY))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ResourceResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    private BookingResponse createBooking(UUID resourceId, ZonedDateTime startsAt, ZonedDateTime endsAt) {
        BookingResponse body = restTestClient
                .post()
                .uri("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateBookingRequest(resourceId, startsAt.toInstant(), endsAt.toInstant()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(BookingResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }
}
