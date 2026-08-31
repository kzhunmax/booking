package com.booking.app.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.app.booking.BookingNotFoundException;
import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.BookingService;
import com.booking.app.booking.BookingStatus;
import com.booking.app.payment.internal.domain.Payment;
import com.booking.app.payment.internal.infrastructure.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final BigDecimal AMOUNT = BigDecimal.valueOf(150.00);
    private static final String CURRENCY = "USD";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    private UUID bookingId;
    private UUID userId;
    private UUID idempotencyKey;
    private BookingResponse pendingBooking;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
        pendingBooking = new BookingResponse(
                bookingId,
                UUID.randomUUID(),
                "user@example.com",
                "John Doe",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                BookingStatus.PENDING,
                AMOUNT,
                CURRENCY);
    }

    @Test
    @DisplayName("Should process successful payment, confirm booking, and return isNew = true")
    void shouldProcessSuccessfulPayment() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bookingService.findByPublicId(bookingId)).thenReturn(pendingBooking);
        when(paymentGateway.charge(AMOUNT, CURRENCY)).thenReturn(new PaymentResult(true, "gw_success_123"));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentExecution execution = paymentService.create(bookingId, userId, idempotencyKey);

        assertThat(execution.isNew()).isTrue();
        assertThat(execution.response().status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(execution.response().bookingId()).isEqualTo(bookingId);
        assertThat(execution.response().amount()).isEqualTo(AMOUNT);
        assertThat(execution.response().currency()).isEqualTo(CURRENCY);
        assertThat(execution.response().gatewayReference()).isEqualTo("gw_success_123");

        verify(bookingService).confirm(bookingId);
        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    @DisplayName("Should process failed payment without confirming booking")
    void shouldProcessFailedPayment() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bookingService.findByPublicId(bookingId)).thenReturn(pendingBooking);
        when(paymentGateway.charge(AMOUNT, CURRENCY)).thenReturn(new PaymentResult(false, "gw_failed_123"));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentExecution execution = paymentService.create(bookingId, userId, idempotencyKey);

        assertThat(execution.isNew()).isTrue();
        assertThat(execution.response().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(execution.response().gatewayReference()).isEqualTo("gw_failed_123");

        verify(bookingService, never()).confirm(any());
        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    @DisplayName("Should return existing payment with isNew = false on repeated request with same idempotency key")
    void shouldReturnExistingPaymentOnRepeatedIdempotencyKey() {
        Payment existingPayment = new Payment(bookingId, userId, AMOUNT, CURRENCY, idempotencyKey);
        existingPayment.markAsSucceeded("gw_existing_ref");
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        PaymentExecution execution = paymentService.create(bookingId, userId, idempotencyKey);

        assertThat(execution.isNew()).isFalse();
        assertThat(execution.response().publicId()).isEqualTo(existingPayment.getPublicId());
        assertThat(execution.response().status()).isEqualTo(PaymentStatus.SUCCEEDED);

        verify(paymentGateway, never()).charge(any(), any());
        verify(bookingService, never()).confirm(any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should throw IdempotencyConflictException when idempotency key is reused for different bookingId")
    void shouldThrowIdempotencyConflictWhenKeyReusedForDifferentBooking() {
        UUID differentBookingId = UUID.randomUUID();
        Payment existingPayment = new Payment(differentBookingId, userId, AMOUNT, CURRENCY, idempotencyKey);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.create(bookingId, userId, idempotencyKey))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessage("Idempotency key '%s' was already used for different booking '%s'"
                        .formatted(idempotencyKey, differentBookingId));

        verify(paymentGateway, never()).charge(any(), any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should throw BookingNotPendingException when booking is not in PENDING status")
    void shouldThrowBookingNotPendingExceptionWhenBookingNotPending() {
        BookingResponse confirmedBooking = new BookingResponse(
                bookingId,
                UUID.randomUUID(),
                "user@example.com",
                "John",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                BookingStatus.CONFIRMED,
                AMOUNT,
                CURRENCY);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bookingService.findByPublicId(bookingId)).thenReturn(confirmedBooking);

        assertThatThrownBy(() -> paymentService.create(bookingId, userId, idempotencyKey))
                .isInstanceOf(BookingNotPendingException.class)
                .hasMessage("Booking %s status is CONFIRMED, expected PENDING".formatted(bookingId));

        verify(paymentGateway, never()).charge(any(), any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should throw BookingNotFoundException when booking does not exist")
    void shouldThrowBookingNotFoundExceptionWhenBookingNotFound() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bookingService.findByPublicId(bookingId)).thenThrow(new BookingNotFoundException(bookingId));

        assertThatThrownBy(() -> paymentService.create(bookingId, userId, idempotencyKey))
                .isInstanceOf(BookingNotFoundException.class);

        verify(paymentGateway, never()).charge(any(), any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should find payment by publicId")
    void shouldFindPaymentByPublicId() {
        Payment payment = new Payment(bookingId, userId, AMOUNT, CURRENCY, idempotencyKey);
        UUID publicId = payment.getPublicId();
        when(paymentRepository.findByPublicId(publicId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.findByPublicId(publicId);

        assertThat(response.publicId()).isEqualTo(publicId);
        assertThat(response.bookingId()).isEqualTo(bookingId);
        verify(paymentRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when payment does not exist")
    void shouldThrowPaymentNotFoundExceptionWhenNotFound() {
        UUID publicId = UUID.randomUUID();
        when(paymentRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findByPublicId(publicId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment with id %s not found".formatted(publicId));
    }

    @Test
    @DisplayName("Should find all payments for booking with pagination")
    void shouldFindAllPaymentsForBooking() {
        Payment payment = new Payment(bookingId, userId, AMOUNT, CURRENCY, idempotencyKey);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(paymentRepository.findByBookingId(eq(bookingId), eq(pageable))).thenReturn(page);

        Page<PaymentResponse> result = paymentService.findAllPayments(bookingId, pageable);

        assertThat(result.getTotalElements()).isOne();
        assertThat(result.getContent().getFirst().bookingId()).isEqualTo(bookingId);
        verify(paymentRepository).findByBookingId(bookingId, pageable);
    }
}
