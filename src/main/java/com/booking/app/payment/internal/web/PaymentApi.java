package com.booking.app.payment.internal.web;

import com.booking.app.common.security.SecurityUser;
import com.booking.app.payment.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Payments", description = "Endpoints for processing idempotency-protected payments for bookings")
@RequestMapping("/api/payments")
public interface PaymentApi {

    @Operation(
            summary = "Execute payment for booking",
            description =
                    "Charges customer for a pending booking. Protected with an Idempotency-Key header to prevent duplicate charges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created and processed successfully"),
        @ApiResponse(
                responseCode = "200",
                description = "Existing payment result returned for duplicate idempotency key"),
        @ApiResponse(responseCode = "400", description = "Invalid payload or booking not in PENDING state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Idempotency conflict - Key used with different parameters")
    })
    @PostMapping
    ResponseEntity<PaymentResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(
                            description = "Unique client-generated UUID guaranteeing exactly-once execution",
                            example = "e1eebc99-9c0b-4ef8-bb6d-6bb9bd380a55",
                            required = true)
                    @RequestHeader("Idempotency-Key")
                    UUID idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request);

    @Operation(
            summary = "Get payment details by ID",
            description = "Retrieves information for a specific payment transaction.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment record found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{publicId}")
    ResponseEntity<PaymentResponse> getById(
            @Parameter(
                            description = "Public UUID of the payment record",
                            example = "d1eebc99-9c0b-4ef8-bb6d-6bb9bd380a44")
                    @PathVariable
                    UUID publicId,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser);

    @Operation(
            summary = "List payment attempts for a booking",
            description = "Retrieves paginated payment transactions recorded for a booking.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of payments returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    Page<PaymentResponse> getPaymentAttempts(
            @Parameter(
                            description = "Booking UUID to fetch payments for",
                            example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
                            required = true)
                    @RequestParam(value = "bookingId")
                    UUID bookingId,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser,
            Pageable pageable);
}
