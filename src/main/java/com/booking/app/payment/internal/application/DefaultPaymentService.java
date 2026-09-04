package com.booking.app.payment.internal.application;

import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.BookingService;
import com.booking.app.booking.BookingStatus;
import com.booking.app.payment.BookingNotPendingException;
import com.booking.app.payment.IdempotencyConflictException;
import com.booking.app.payment.PaymentExecution;
import com.booking.app.payment.PaymentGateway;
import com.booking.app.payment.PaymentNotFoundException;
import com.booking.app.payment.PaymentResponse;
import com.booking.app.payment.PaymentResult;
import com.booking.app.payment.PaymentService;
import com.booking.app.payment.PaymentStatus;
import com.booking.app.payment.internal.domain.Payment;
import com.booking.app.payment.internal.infrastructure.PaymentMapper;
import com.booking.app.payment.internal.infrastructure.PaymentRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaymentService.class);
    private static final String UNIQUE_IDEMPOTENCY_CONSTRAINT = "uc_payments_idempotency_key";

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final PaymentGateway paymentGateway;

    public DefaultPaymentService(
            PaymentRepository paymentRepository, BookingService bookingService, PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.paymentGateway = paymentGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByPublicId(UUID publicId, UUID callerUserId) {
        log.debug("Fetching payment by publicId={}", publicId);
        return PaymentMapper.toResponse(requirePayment(publicId, callerUserId));
    }

    @Override
    public PaymentExecution create(UUID bookingId, UUID userId, UUID idempotencyKey) {
        Payment existingPayment = checkIdempotency(bookingId, idempotencyKey);
        if (existingPayment != null) {
            return new PaymentExecution(PaymentMapper.toResponse(existingPayment), false);
        }
        BookingResponse booking = fetchAndValidateBooking(bookingId);
        Payment payment = createAndChargePayment(booking, userId, idempotencyKey);
        Payment savedPayment = persist(payment);
        log.info("Processed payment: bookingId={}, userId={}, idempotencyKey={}", bookingId, userId, idempotencyKey);
        boolean isNew = savedPayment.equals(payment);
        return new PaymentExecution(PaymentMapper.toResponse(savedPayment), isNew);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAllPayments(UUID bookingId, UUID callerUserId, Pageable pageable) {
        Page<Payment> payments = callerUserId == null
                ? paymentRepository.findByBookingId(bookingId, pageable)
                : paymentRepository.findByBookingIdAndUserId(bookingId, callerUserId, pageable);
        return payments.map(PaymentMapper::toResponse);
    }

    private Payment requirePayment(UUID publicId, UUID callerUserId) {
        if (callerUserId == null) {
            return paymentRepository.findByPublicId(publicId).orElseThrow(() -> new PaymentNotFoundException(publicId));
        }
        return paymentRepository
                .findByPublicIdAndUserId(publicId, callerUserId)
                .orElseThrow(() -> new PaymentNotFoundException(publicId));
    }

    private Payment checkIdempotency(UUID bookingId, UUID idempotencyKey) {
        Optional<Payment> optionalPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (optionalPayment.isEmpty()) {
            return null;
        }
        Payment existing = optionalPayment.get();
        if (existing.getBookingId().equals(bookingId)) {
            return existing;
        }
        throw new IdempotencyConflictException(idempotencyKey, existing.getBookingId());
    }

    private BookingResponse fetchAndValidateBooking(UUID bookingId) {
        BookingResponse booking = bookingService.findByPublicId(bookingId);
        if (booking.status() != BookingStatus.PENDING) {
            throw new BookingNotPendingException(bookingId, booking.status());
        }
        return booking;
    }

    private Payment createAndChargePayment(BookingResponse booking, UUID userId, UUID idempotencyKey) {
        Payment payment =
                new Payment(booking.publicId(), userId, booking.totalAmount(), booking.currency(), idempotencyKey);
        PaymentResult result = paymentGateway.charge(booking.totalAmount(), booking.currency());
        if (result.isSuccess()) {
            payment.markAsSucceeded(result.gatewayReference());
        } else {
            payment.markAsFailed(result.gatewayReference());
        }
        return payment;
    }

    private Payment persist(Payment payment) {
        try {
            Payment saved = paymentRepository.saveAndFlush(payment);
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                bookingService.confirm(payment.getBookingId());
            }
            return saved;
        } catch (DataIntegrityViolationException ex) {
            if (isIdempotencyKeyViolation(ex)) {
                log.warn(
                        "Payment with idempotency key '{}' already persisted concurrently",
                        payment.getIdempotencyKey());
                Payment existing = checkIdempotency(payment.getBookingId(), payment.getIdempotencyKey());
                if (existing == null) {
                    throw new IdempotencyConflictException(
                            "Idempotency key present in DB but fetch failed after conflict", ex);
                }
                return existing;
            }
            throw ex;
        }
    }

    private boolean isIdempotencyKeyViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(UNIQUE_IDEMPOTENCY_CONSTRAINT);
    }
}
