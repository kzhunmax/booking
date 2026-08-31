package com.booking.app.payment.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.app.booking.BookingStatus;
import com.booking.app.payment.BookingNotPendingException;
import com.booking.app.payment.IdempotencyConflictException;
import com.booking.app.payment.PaymentExecution;
import com.booking.app.payment.PaymentNotFoundException;
import com.booking.app.payment.PaymentResponse;
import com.booking.app.payment.PaymentService;
import com.booking.app.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest {

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.valueOf(100.0);
    private static final String DEFAULT_CURRENCY = "USD";

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/payments - returns 201 Created and Location header on new payment")
    void shouldCreatePaymentSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        CreatePaymentRequest request = new CreatePaymentRequest(bookingId, userId);
        PaymentResponse response = new PaymentResponse(
                publicId,
                bookingId,
                userId,
                DEFAULT_AMOUNT,
                DEFAULT_CURRENCY,
                PaymentStatus.SUCCEEDED,
                idempotencyKey,
                "gw_ref_123");
        PaymentExecution execution = new PaymentExecution(response, true);

        when(paymentService.create(bookingId, userId, idempotencyKey)).thenReturn(execution);

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/payments/" + publicId))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey.toString()))
                .andExpect(jsonPath("$.gatewayReference").value("gw_ref_123"));

        verify(paymentService).create(bookingId, userId, idempotencyKey);
    }

    @Test
    @DisplayName("POST /api/payments - returns 200 OK without Location header on idempotent replay")
    void shouldReturnOkOnIdempotentReplay() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        CreatePaymentRequest request = new CreatePaymentRequest(bookingId, userId);
        PaymentResponse response = new PaymentResponse(
                publicId,
                bookingId,
                userId,
                DEFAULT_AMOUNT,
                DEFAULT_CURRENCY,
                PaymentStatus.SUCCEEDED,
                idempotencyKey,
                "gw_ref_123");
        PaymentExecution execution = new PaymentExecution(response, false);

        when(paymentService.create(bookingId, userId, idempotencyKey)).thenReturn(execution);

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        verify(paymentService).create(bookingId, userId, idempotencyKey);
    }

    @Test
    @DisplayName("POST /api/payments - returns 400 Bad Request when Idempotency-Key header is missing")
    void shouldReturnBadRequestWhenIdempotencyHeaderIsMissing() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/payments - returns 400 Bad Request when Idempotency-Key is not a UUID")
    void shouldReturnBadRequestWhenIdempotencyHeaderIsNotUuid() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "not-a-valid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/payments - returns 400 Bad Request when request body has null bookingId")
    void shouldReturnBadRequestWhenBookingIdIsNull() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        String json = "{\"bookingId\":null,\"userId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/payments - returns 409 Conflict when booking is not in PENDING status")
    void shouldReturnConflictWhenBookingNotPending() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(bookingId, userId);

        when(paymentService.create(bookingId, userId, idempotencyKey))
                .thenThrow(new BookingNotPendingException(bookingId, BookingStatus.CONFIRMED));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Booking Not Pending"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName(
            "POST /api/payments - returns 422 Unprocessable Content when idempotency key reused for different booking")
    void shouldReturnUnprocessableWhenKeyReusedForDifferentBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(bookingId, userId);

        when(paymentService.create(bookingId, userId, idempotencyKey))
                .thenThrow(new IdempotencyConflictException(idempotencyKey, UUID.randomUUID()));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Idempotency Conflict"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("GET /api/payments/{publicId} - returns 200 OK when payment found")
    void shouldReturnPaymentWhenFoundByPublicId() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                publicId,
                bookingId,
                userId,
                DEFAULT_AMOUNT,
                DEFAULT_CURRENCY,
                PaymentStatus.SUCCEEDED,
                idempotencyKey,
                "gw_ref");

        when(paymentService.findByPublicId(publicId)).thenReturn(response);

        mockMvc.perform(get("/api/payments/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        verify(paymentService).findByPublicId(publicId);
    }

    @Test
    @DisplayName("GET /api/payments/{publicId} - returns 404 Not Found when payment missing")
    void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(paymentService.findByPublicId(publicId)).thenThrow(new PaymentNotFoundException(publicId));

        mockMvc.perform(get("/api/payments/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Payment Not Found"))
                .andExpect(jsonPath("$.status").value(404));

        verify(paymentService).findByPublicId(publicId);
    }

    @Test
    @DisplayName("GET /api/payments/{publicId} - returns 400 Bad Request when publicId is not a UUID")
    void shouldReturnBadRequestWhenPublicIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/payments/{publicId}", "not-a-uuid")).andExpect(status().isBadRequest());

        verify(paymentService, never()).findByPublicId(any());
    }

    @Test
    @DisplayName("GET /api/payments?bookingId={bookingId} - returns 200 OK with paginated list")
    void shouldReturnPaymentAttemptsForBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                publicId,
                bookingId,
                UUID.randomUUID(),
                DEFAULT_AMOUNT,
                DEFAULT_CURRENCY,
                PaymentStatus.SUCCEEDED,
                UUID.randomUUID(),
                "gw_ref");

        Page<PaymentResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
        when(paymentService.findAllPayments(eq(bookingId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/payments").param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.content[0].bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(paymentService).findAllPayments(eq(bookingId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/payments - returns 400 Bad Request when bookingId parameter is missing")
    void shouldReturnBadRequestWhenBookingIdParamMissing() throws Exception {
        mockMvc.perform(get("/api/payments")).andExpect(status().isBadRequest());

        verify(paymentService, never()).findAllPayments(any(), any());
    }
}
