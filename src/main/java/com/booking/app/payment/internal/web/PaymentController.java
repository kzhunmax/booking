package com.booking.app.payment.internal.web;

import com.booking.app.common.security.SecurityUser;
import com.booking.app.payment.PaymentExecution;
import com.booking.app.payment.PaymentResponse;
import com.booking.app.payment.PaymentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public ResponseEntity<PaymentResponse> create(
            SecurityUser currentUser, UUID idempotencyKey, CreatePaymentRequest request) {
        UUID effectiveUserId = currentUser.isAdmin() ? request.userId() : currentUser.publicId();
        PaymentExecution execution = paymentService.create(request.bookingId(), effectiveUserId, idempotencyKey);
        if (!execution.isNew()) {
            return ResponseEntity.ok(execution.response());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(execution.response().publicId())
                .toUri();
        return ResponseEntity.created(location).body(execution.response());
    }

    @Override
    public ResponseEntity<PaymentResponse> getById(UUID publicId, SecurityUser currentUser) {
        UUID callerUserId = currentUser.isAdmin() ? null : currentUser.publicId();
        return ResponseEntity.ok(paymentService.findByPublicId(publicId, callerUserId));
    }

    @Override
    public Page<PaymentResponse> getPaymentAttempts(
            UUID bookingId,
            SecurityUser currentUser,
            @PageableDefault(size = 20, sort = "auditInfo.createdAt", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        UUID callerUserId = currentUser.isAdmin() ? null : currentUser.publicId();
        return paymentService.findAllPayments(bookingId, callerUserId, pageable);
    }
}
