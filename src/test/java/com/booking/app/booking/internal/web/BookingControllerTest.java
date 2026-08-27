package com.booking.app.booking.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.CancellationTooLateException;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {

    private static final Instant STARTS_AT = Instant.parse("2026-09-01T14:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-09-01T15:00:00Z");

    @MockitoBean
    BookingService bookingService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/bookings - returns 201 Created and location header when request is valid")
    void shouldCreateBookingSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        CreateBookingRequest request =
                new CreateBookingRequest(resourceId, "customer@example.com", "John Doe", STARTS_AT, ENDS_AT);
        when(bookingService.create(resourceId, "customer@example.com", "John Doe", STARTS_AT, ENDS_AT))
                .thenReturn(bookingResponse(publicId, resourceId, BookingStatus.PENDING));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/bookings/" + publicId))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$.customerEmail").value("customer@example.com"))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.startsAt").value(STARTS_AT.toString()))
                .andExpect(jsonPath("$.endsAt").value(ENDS_AT.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(bookingService).create(resourceId, "customer@example.com", "John Doe", STARTS_AT, ENDS_AT);
    }

    @Test
    @DisplayName("POST /api/bookings - returns 400 when email is blank")
    void shouldReturnBadRequestWhenCreateEmailIsBlank() throws Exception {
        CreateBookingRequest request =
                new CreateBookingRequest(UUID.randomUUID(), "   ", "John Doe", STARTS_AT, ENDS_AT);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/bookings - returns 400 when endsAt is not after startsAt")
    void shouldReturnBadRequestWhenIntervalIsInvalid() throws Exception {
        CreateBookingRequest request =
                new CreateBookingRequest(UUID.randomUUID(), "customer@example.com", "John Doe", STARTS_AT, STARTS_AT);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/bookings/{publicId} - returns 200 OK with booking payload")
    void shouldReturnBookingWhenFoundByPublicId() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(bookingService.findByPublicId(publicId))
                .thenReturn(bookingResponse(publicId, resourceId, BookingStatus.PENDING));

        mockMvc.perform(get("/api/bookings/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(bookingService).findByPublicId(publicId);
    }

    @Test
    @DisplayName("GET /api/bookings/{publicId} - returns 404 when booking does not exist")
    void shouldReturnNotFoundWhenBookingDoesNotExist() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.findByPublicId(publicId)).thenThrow(new BookingNotFoundException(publicId));

        mockMvc.perform(get("/api/bookings/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Booking Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Booking with id %s not found".formatted(publicId)));

        verify(bookingService).findByPublicId(publicId);
    }

    @Test
    @DisplayName("GET /api/bookings/{publicId} - returns 400 when publicId is not a UUID")
    void shouldReturnBadRequestWhenPublicIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/bookings/{publicId}", "not-a-uuid")).andExpect(status().isBadRequest());

        verify(bookingService, never()).findByPublicId(any());
    }

    @Test
    @DisplayName("GET /api/bookings - returns 200 OK with default pagination when filters are omitted")
    void shouldReturnBookingsWithDefaultPaginationWhenFiltersNotProvided() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Page<BookingResponse> page = new PageImpl<>(
                List.of(bookingResponse(publicId, resourceId, BookingStatus.PENDING)), PageRequest.of(0, 20), 1);
        when(bookingService.findAll(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(bookingService).findAll(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/bookings - returns 200 OK with filtered bookings")
    void shouldReturnFilteredBookingsWhenQueryParamsProvided() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");
        Page<BookingResponse> page = new PageImpl<>(
                List.of(bookingResponse(publicId, resourceId, BookingStatus.CONFIRMED)), PageRequest.of(0, 20), 1);
        when(bookingService.findAll(eq(resourceId), eq(BookingStatus.CONFIRMED), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/bookings")
                        .param("resourceId", resourceId.toString())
                        .param("status", "CONFIRMED")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(bookingService)
                .findAll(eq(resourceId), eq(BookingStatus.CONFIRMED), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/bookings?status=UNKNOWN - returns 400 when status is not a valid enum value")
    void shouldReturnBadRequestWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/bookings").param("status", "UNKNOWN")).andExpect(status().isBadRequest());

        verify(bookingService, never()).findAll(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 200 OK with cancelled booking")
    void shouldCancelBookingSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(bookingService.cancel(publicId))
                .thenReturn(bookingResponse(publicId, resourceId, BookingStatus.CANCELLED));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(bookingService).cancel(publicId);
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 404 when booking does not exist")
    void shouldReturnNotFoundWhenCancellingMissingBooking() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.cancel(publicId)).thenThrow(new BookingNotFoundException(publicId));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Booking Not Found"));
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 409 when cancellation is too late")
    void shouldReturnConflictWhenCancellationIsTooLate() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.cancel(publicId))
                .thenThrow(new CancellationTooLateException("Cannot cancel later than 2 hours before start"));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cancellation Too Late"))
                .andExpect(jsonPath("$.detail").value("Cannot cancel later than 2 hours before start"));
    }

    @Test
    @DisplayName("POST /api/bookings/{publicId}/cancel - returns 422 when booking is already completed")
    void shouldReturnUnprocessableWhenCancellingCompletedBooking() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(bookingService.cancel(publicId))
                .thenThrow(new BookingAlreadyCompletedException(
                        "Cannot cancel a booking that has already been completed"));

        mockMvc.perform(post("/api/bookings/{publicId}/cancel", publicId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Booking Already Completed"));
    }

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

    private static BookingResponse bookingResponse(UUID publicId, UUID resourceId, BookingStatus status) {
        return new BookingResponse(
                publicId, resourceId, "customer@example.com", "John Doe", STARTS_AT, ENDS_AT, status);
    }
}
