package com.booking.app.booking.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.app.booking.AvailableSlotsResponse;
import com.booking.app.booking.BookingAlreadyCompletedException;
import com.booking.app.booking.BookingNotFoundException;
import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.BookingService;
import com.booking.app.booking.BookingSlotAlreadyTakenException;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.CancellationTooLateException;
import com.booking.app.common.security.SecurityUser;
import com.booking.app.config.TestSecurityConfig;
import com.booking.app.resource.ResourceCurrentlyNotAvailableException;
import com.booking.app.resource.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = BookingController.class)
@Import(TestSecurityConfig.class)
class BookingControllerTest {

    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String CUSTOMER_NAME = "John Doe";
    private static final UUID CUSTOMER_PUBLIC_ID = UUID.randomUUID();
    private static final Instant STARTS_AT = Instant.parse("2026-09-01T14:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-09-01T15:00:00Z");

    @MockitoBean
    BookingService bookingService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // POST /api/bookings
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/bookings - returns 201 Created and location header when request is valid")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldCreateBookingSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        CreateBookingRequest request = new CreateBookingRequest(resourceId, STARTS_AT, ENDS_AT);
        when(bookingService.create(eq(resourceId), any(), any(), eq(STARTS_AT), eq(ENDS_AT)))
                .thenReturn(bookingResponse(publicId, resourceId, BookingStatus.PENDING));

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/bookings/" + publicId))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$.customerEmail").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.customerName").value(CUSTOMER_NAME))
                .andExpect(jsonPath("$.startsAt").value(STARTS_AT.toString()))
                .andExpect(jsonPath("$.endsAt").value(ENDS_AT.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(100.0))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("POST /api/bookings - returns 400 when endsAt is not after startsAt")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnBadRequestWhenIntervalIsInvalid() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(UUID.randomUUID(), STARTS_AT, STARTS_AT);

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/bookings - returns 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(UUID.randomUUID(), STARTS_AT, ENDS_AT);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(bookingService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/bookings - returns 404 when resource does not exist")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnNotFoundWhenCreatingBookingForMissingResource() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateBookingRequest request = new CreateBookingRequest(resourceId, STARTS_AT, ENDS_AT);
        when(bookingService.create(eq(resourceId), any(), any(), eq(STARTS_AT), eq(ENDS_AT)))
                .thenThrow(new ResourceNotFoundException(resourceId));

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("POST /api/bookings - returns 422 when resource is not active")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnUnprocessableWhenCreatingBookingForInactiveResource() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateBookingRequest request = new CreateBookingRequest(resourceId, STARTS_AT, ENDS_AT);
        when(bookingService.create(eq(resourceId), any(), any(), eq(STARTS_AT), eq(ENDS_AT)))
                .thenThrow(new ResourceCurrentlyNotAvailableException(
                        "Resource '%s' is not available for booking".formatted(resourceId)));

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Resource Not Available"));
    }

    @Test
    @DisplayName("POST /api/bookings - returns 409 when slot is already taken")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnConflictWhenCreatingOverlappingBooking() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateBookingRequest request = new CreateBookingRequest(resourceId, STARTS_AT, ENDS_AT);
        when(bookingService.create(eq(resourceId), any(), any(), eq(STARTS_AT), eq(ENDS_AT)))
                .thenThrow(new BookingSlotAlreadyTakenException(STARTS_AT, ENDS_AT, new RuntimeException("overlap")));

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Booking Slot Taken"));
    }

    @Test
    @DisplayName("POST /api/bookings - returns 400 when domain rejects the argument")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnBadRequestWhenServiceThrowsIllegalArgumentException() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateBookingRequest request = new CreateBookingRequest(resourceId, STARTS_AT, ENDS_AT);
        when(bookingService.create(eq(resourceId), any(), any(), eq(STARTS_AT), eq(ENDS_AT)))
                .thenThrow(new IllegalArgumentException("Booking start must be in the future"));

        mockMvc.perform(post("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Argument"))
                .andExpect(jsonPath("$.detail").value("Booking start must be in the future"));
    }

    // -------------------------------------------------------------------------
    // GET /api/bookings/{publicId}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/bookings/{publicId} - returns 200 OK with booking payload")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnBookingWhenFoundByPublicId() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(bookingService.findByPublicId(eq(publicId), eq(CUSTOMER_EMAIL), eq(false)))
                .thenReturn(bookingResponse(publicId, resourceId, BookingStatus.PENDING));

        mockMvc.perform(get("/api/bookings/{publicId}", publicId)
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(bookingService).findByPublicId(publicId, CUSTOMER_EMAIL, false);
    }

    @Test
    @DisplayName("GET /api/bookings/{publicId} - admin can see any booking")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldReturnBookingForAdmin() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(bookingService.findByPublicId(eq(publicId), any(), eq(true)))
                .thenReturn(bookingResponse(publicId, resourceId, BookingStatus.PENDING));

        mockMvc.perform(get("/api/bookings/{publicId}", publicId)
                        .with(user(mockSecurityUser(UUID.randomUUID(), "admin@example.com", "Admin", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()));
    }

    @Test
    @DisplayName("GET /api/bookings/{publicId} - returns 404 when booking does not exist")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnNotFoundWhenBookingDoesNotExist() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.findByPublicId(eq(publicId), any(), anyBoolean()))
                .thenThrow(new BookingNotFoundException(publicId));

        mockMvc.perform(get("/api/bookings/{publicId}", publicId)
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Booking Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Booking with id %s not found".formatted(publicId)));
    }

    @Test
    @DisplayName("GET /api/bookings/{publicId} - returns 400 when publicId is not a UUID")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnBadRequestWhenPublicIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/bookings/{publicId}", "not-a-uuid")).andExpect(status().isBadRequest());

        verify(bookingService, never()).findByPublicId(any(), any(), anyBoolean());
    }

    // -------------------------------------------------------------------------
    // GET /api/bookings
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/bookings - admin sees all bookings (no customerEmail filter)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldReturnAllBookingsForAdmin() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Page<BookingResponse> page = new PageImpl<>(
                List.of(bookingResponse(publicId, resourceId, BookingStatus.PENDING)), PageRequest.of(0, 20), 1);
        // admin → customerEmail is null in findAll
        when(bookingService.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/bookings")
                        .with(user(mockSecurityUser(UUID.randomUUID(), "admin@example.com", "Admin", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings - customer only sees own bookings (customerEmail filter applied)")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnOnlyOwnBookingsForCustomer() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Page<BookingResponse> page = new PageImpl<>(
                List.of(bookingResponse(publicId, resourceId, BookingStatus.PENDING)), PageRequest.of(0, 20), 1);
        when(bookingService.findAll(isNull(), eq(CUSTOMER_EMAIL), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings - returns filtered bookings when query params are provided")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnFilteredBookingsWhenQueryParamsProvided() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");
        Page<BookingResponse> page = new PageImpl<>(
                List.of(bookingResponse(publicId, resourceId, BookingStatus.CONFIRMED)), PageRequest.of(0, 20), 1);
        when(bookingService.findAll(
                        eq(resourceId), any(), eq(BookingStatus.CONFIRMED), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .param("resourceId", resourceId.toString())
                        .param("status", "CONFIRMED")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings?status=UNKNOWN - returns 400 when status is not a valid enum value")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnBadRequestWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false)))
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).findAll(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/bookings - returns 200 OK with empty page when no bookings match")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnEmptyPageWhenNoBookingsMatch() throws Exception {
        when(bookingService.findAll(isNull(), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/bookings")
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // POST /api/bookings/{publicId}/cancel
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 200 OK with cancelled booking")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldCancelBookingSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(bookingService.cancel(eq(publicId), eq(CUSTOMER_EMAIL), eq(false)))
                .thenReturn(bookingResponse(publicId, resourceId, BookingStatus.CANCELLED));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId)
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(bookingService).cancel(publicId, CUSTOMER_EMAIL, false);
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 404 when booking does not exist")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnNotFoundWhenCancellingMissingBooking() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.cancel(eq(publicId), any(), anyBoolean()))
                .thenThrow(new BookingNotFoundException(publicId));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId)
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Booking Not Found"));
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 409 when cancellation is too late")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnConflictWhenCancellationIsTooLate() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.cancel(eq(publicId), any(), anyBoolean()))
                .thenThrow(new CancellationTooLateException("Cannot cancel later than 2 hours before start"));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId)
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cancellation Too Late"))
                .andExpect(jsonPath("$.detail").value("Cannot cancel later than 2 hours before start"));
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 409 when another transaction updated the booking")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnConflictWhenCancellingBookingWithStaleVersion() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.cancel(eq(publicId), any(), anyBoolean()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Booking", publicId));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId)
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent Modification Conflict"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "The entity was updated or deleted by another transaction. Please refresh and try again."));
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 422 when booking is already completed")
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void shouldReturnUnprocessableWhenCancellingCompletedBooking() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.cancel(eq(publicId), any(), anyBoolean()))
                .thenThrow(new BookingAlreadyCompletedException(
                        "Cannot cancel a booking that has already been completed"));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId)
                        .with(user(mockSecurityUser(CUSTOMER_PUBLIC_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, false))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Booking Already Completed"));
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 401 when not authenticated")
    void shouldReturnUnauthorizedWhenCancellingWithoutAuthentication() throws Exception {
        UUID publicId = UUID.randomUUID();

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId)).andExpect(status().isUnauthorized());

        verify(bookingService, never()).cancel(any(), any(), anyBoolean());
    }

    // -------------------------------------------------------------------------
    // GET /api/bookings/available-slots
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/bookings/available-slots - returns 200 OK with free intervals")
    void shouldReturnAvailableSlots() throws Exception {
        UUID resourceId = UUID.randomUUID();
        LocalDate date = LocalDate.parse("2026-09-01");
        AvailableSlotsResponse response = new AvailableSlotsResponse(
                resourceId, date, List.of(new AvailableSlotsResponse.TimeSlot(STARTS_AT, ENDS_AT)));
        when(bookingService.findAvailableSlots(resourceId, date)).thenReturn(response);

        mockMvc.perform(get("/api/bookings/available-slots")
                        .param("resourceId", resourceId.toString())
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$.date").value(date.toString()))
                .andExpect(jsonPath("$.slots[0].startsAt").value(STARTS_AT.toString()))
                .andExpect(jsonPath("$.slots[0].endsAt").value(ENDS_AT.toString()));

        verify(bookingService).findAvailableSlots(resourceId, date);
    }

    @Test
    @DisplayName("GET /api/bookings/available-slots - returns 400 when resourceId is missing")
    void shouldReturnBadRequestWhenAvailableSlotsResourceIdIsMissing() throws Exception {
        mockMvc.perform(get("/api/bookings/available-slots").param("date", "2026-09-01"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).findAvailableSlots(any(), any());
    }

    @Test
    @DisplayName("GET /api/bookings/available-slots - returns 400 when date is missing")
    void shouldReturnBadRequestWhenAvailableSlotsDateIsMissing() throws Exception {
        mockMvc.perform(get("/api/bookings/available-slots")
                        .param("resourceId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).findAvailableSlots(any(), any());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static SecurityUser mockSecurityUser(UUID publicId, String email, String name, boolean isAdmin) {
        String role = isAdmin ? "ADMIN" : "CUSTOMER";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        return new SecurityUser(publicId, email, "hash", authorities, name, true);
    }

    private static BookingResponse bookingResponse(UUID publicId, UUID resourceId, BookingStatus status) {
        return new BookingResponse(
                publicId,
                resourceId,
                CUSTOMER_EMAIL,
                CUSTOMER_NAME,
                STARTS_AT,
                ENDS_AT,
                status,
                BigDecimal.valueOf(100.0),
                "USD");
    }
}
