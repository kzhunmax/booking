package com.booking.app.payment;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponse findByPublicId(UUID publicId);

    PaymentExecution create(UUID bookingId, UUID userId, UUID idempotencyKey);

    Page<PaymentResponse> findAllPayments(UUID bookingId, Pageable pageable);
}
