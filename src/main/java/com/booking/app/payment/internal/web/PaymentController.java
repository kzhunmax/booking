package com.booking.app.payment.internal.web;

import com.booking.app.payment.PaymentExecution;
import com.booking.app.payment.PaymentResponse;
import com.booking.app.payment.PaymentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey, @Valid @RequestBody CreatePaymentRequest request) {
        PaymentExecution execution = paymentService.create(request.bookingId(), request.userId(), idempotencyKey);
        if (!execution.isNew()) {
            return ResponseEntity.ok(execution.response());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(execution.response().publicId())
                .toUri();
        return ResponseEntity.created(location).body(execution.response());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID publicId) {
        return ResponseEntity.ok(paymentService.findByPublicId(publicId));
    }

    @GetMapping
    public Page<PaymentResponse> getPaymentAttempts(
            @RequestParam(value = "bookingId") UUID bookingId,
            @PageableDefault(size = 20, sort = "auditInfo.createdAt", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return paymentService.findAllPayments(bookingId, pageable);
    }
}
