package com.booking.app.payment.internal.infrastructure;

import com.booking.app.payment.internal.domain.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPublicId(UUID publicId);

    Optional<Payment> findByPublicIdAndUserId(UUID publicId, UUID userId);

    Optional<Payment> findByIdempotencyKey(UUID idempotencyKey);

    Page<Payment> findByBookingId(UUID bookingId, Pageable pageable);

    Page<Payment> findByBookingIdAndUserId(UUID bookingId, UUID userId, Pageable pageable);
}
