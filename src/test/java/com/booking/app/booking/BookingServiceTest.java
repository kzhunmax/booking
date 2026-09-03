package com.booking.app.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.app.booking.internal.application.DefaultBookingService;
import com.booking.app.booking.internal.domain.Booking;
import com.booking.app.booking.internal.domain.BookingInterval;
import com.booking.app.booking.internal.domain.BookingPricing;
import com.booking.app.booking.internal.domain.CustomerDetails;
import com.booking.app.booking.internal.infrastructure.BookingRepository;
import com.booking.app.notification.NotificationService;
import com.booking.app.resource.ResourceCurrentlyNotAvailableException;
import com.booking.app.resource.ResourceNotFoundException;
import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceService;
import com.booking.app.resource.ResourceStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant STARTS_AT = NOW.plus(Duration.ofHours(4));
    private static final Instant ENDS_AT = NOW.plus(Duration.ofHours(5));
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String CUSTOMER_NAME = "John Doe";

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceService resourceService;

    @Mock
    private NotificationService notificationService;

    private BookingService bookingService;
    private UUID resourceId;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        bookingService = new DefaultBookingService(
                bookingRepository, Clock.fixed(NOW, ZoneOffset.UTC), resourceService, notificationService);
        resourceId = UUID.randomUUID();
        testBooking = newBooking(resourceId, STARTS_AT, ENDS_AT);
    }

    @Test
    @DisplayName("Should create booking in PENDING status when resource is active")
    void shouldCreateBookingWhenResourceIsActive() {
        ResourceResponse resourceResponse = new ResourceResponse(
                resourceId, "Conference Room", "desc", ResourceStatus.ACTIVE, BigDecimal.valueOf(100.00), "USD");
        when(resourceService.requireActive(resourceId)).thenReturn(resourceResponse);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.create(resourceId, CUSTOMER_EMAIL, CUSTOMER_NAME, STARTS_AT, ENDS_AT);

        assertThat(response.publicId()).isNotNull();
        assertThat(response.resourceId()).isEqualTo(resourceId);
        assertThat(response.customerEmail()).isEqualTo(CUSTOMER_EMAIL);
        assertThat(response.customerName()).isEqualTo(CUSTOMER_NAME);
        assertThat(response.startsAt()).isEqualTo(STARTS_AT);
        assertThat(response.endsAt()).isEqualTo(ENDS_AT);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(response.currency()).isEqualTo("USD");
        verify(resourceService).requireActive(resourceId);
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating a booking for a missing resource")
    void shouldThrowWhenCreatingBookingForMissingResource() {
        doThrow(new ResourceNotFoundException(resourceId)).when(resourceService).requireActive(resourceId);

        assertThatThrownBy(() -> bookingService.create(resourceId, CUSTOMER_EMAIL, CUSTOMER_NAME, STARTS_AT, ENDS_AT))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw ResourceCurrentlyNotAvailableException when creating a booking for an inactive resource")
    void shouldThrowWhenCreatingBookingForInactiveResource() {
        doThrow(new ResourceCurrentlyNotAvailableException("inactive"))
                .when(resourceService)
                .requireActive(resourceId);

        assertThatThrownBy(() -> bookingService.create(resourceId, CUSTOMER_EMAIL, CUSTOMER_NAME, STARTS_AT, ENDS_AT))
                .isInstanceOf(ResourceCurrentlyNotAvailableException.class);
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BookingSlotAlreadyTakenException when overlapping constraint is violated")
    void shouldThrowBookingSlotAlreadyTakenExceptionWhenSlotOverlaps() {
        ResourceResponse resourceResponse = new ResourceResponse(
                resourceId, "Conference Room", "desc", ResourceStatus.ACTIVE, BigDecimal.valueOf(100.00), "USD");
        when(resourceService.requireActive(resourceId)).thenReturn(resourceResponse);
        Throwable cause = new RuntimeException("no_overlapping_bookings");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("overlap", cause);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenThrow(ex);

        assertThatThrownBy(() -> bookingService.create(resourceId, CUSTOMER_EMAIL, CUSTOMER_NAME, STARTS_AT, ENDS_AT))
                .isInstanceOf(BookingSlotAlreadyTakenException.class);
    }

    @Test
    @DisplayName("Should rethrow DataIntegrityViolationException when overlap constraint message is null")
    void shouldRethrowDataIntegrityViolationExceptionWhenOverlapConstraintMessageIsNull() {
        ResourceResponse resourceResponse = new ResourceResponse(
                resourceId, "Conference Room", "desc", ResourceStatus.ACTIVE, BigDecimal.valueOf(100.00), "USD");
        when(resourceService.requireActive(resourceId)).thenReturn(resourceResponse);
        Throwable cause = new RuntimeException((String) null);
        DataIntegrityViolationException ex = new DataIntegrityViolationException("some error", cause);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenThrow(ex);

        assertThatThrownBy(() -> bookingService.create(resourceId, CUSTOMER_EMAIL, CUSTOMER_NAME, STARTS_AT, ENDS_AT))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should rethrow DataIntegrityViolationException for unexpected constraint")
    void shouldRethrowDataIntegrityViolationExceptionForUnexpectedConstraint() {
        ResourceResponse resourceResponse = new ResourceResponse(
                resourceId, "Conference Room", "desc", ResourceStatus.ACTIVE, BigDecimal.valueOf(100.00), "USD");
        when(resourceService.requireActive(resourceId)).thenReturn(resourceResponse);
        Throwable cause = new RuntimeException("some cause");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("some error", cause);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenThrow(ex);

        assertThatThrownBy(() -> bookingService.create(resourceId, CUSTOMER_EMAIL, CUSTOMER_NAME, STARTS_AT, ENDS_AT))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find booking by publicId")
    void shouldFindBookingByPublicId() {
        UUID publicId = testBooking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.of(testBooking));

        BookingResponse response = bookingService.findByPublicId(publicId);

        assertThat(response.publicId()).isEqualTo(publicId);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(response.currency()).isEqualTo("USD");
        verify(bookingRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Should throw BookingNotFoundException when booking does not exist")
    void shouldThrowBookingNotFoundExceptionWhenBookingDoesNotExist() {
        UUID publicId = testBooking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findByPublicId(publicId)).isInstanceOf(BookingNotFoundException.class);
        verify(bookingRepository).findByPublicId(publicId);
    }

    @ParameterizedTest
    @EnumSource(BookingStatus.class)
    @DisplayName("Should return pageable bookings filtered by status")
    void shouldReturnPageableBookingsFilteredByStatus(BookingStatus status) {
        Booking booking = bookingInStatus(status);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(booking), pageable, 1);
        when(bookingRepository.findAll(anyBookingSpec(), eq(pageable))).thenReturn(page);

        Page<BookingResponse> result = bookingService.findAll(resourceId, status, STARTS_AT, ENDS_AT, pageable);

        assertThat(result.getTotalElements()).isOne();
        assertThat(result.getContent().getFirst().status()).isEqualTo(status);
        verify(bookingRepository).findAll(anyBookingSpec(), eq(pageable));
    }

    @Test
    @DisplayName("Should confirm pending booking successfully")
    void shouldConfirmPendingBookingSuccessfully() {
        UUID publicId = testBooking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.confirm(publicId);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).saveAndFlush(testBooking);
        verify(notificationService)
                .sendBookingConfirmed(publicId, testBooking.getCustomerEmail(), testBooking.getCustomerName());
    }

    @Test
    @DisplayName("Should throw BookingNotFoundException when confirming a missing booking")
    void shouldThrowWhenConfirmingMissingBooking() {
        UUID publicId = UUID.randomUUID();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirm(publicId)).isInstanceOf(BookingNotFoundException.class);
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
        verify(notificationService, never()).sendBookingConfirmed(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(
            value = BookingStatus.class,
            names = {"PENDING", "CONFIRMED"})
    @DisplayName("Should cancel pending or confirmed booking more than 2 hours before start")
    void shouldCancelBookableBooking(BookingStatus status) {
        Booking booking = bookingInStatus(status);
        UUID publicId = booking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.of(booking));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancel(publicId);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).saveAndFlush(booking);
        verify(notificationService)
                .sendBookingCancelled(publicId, booking.getCustomerEmail(), booking.getCustomerName());
    }

    @Test
    @DisplayName("Should be idempotent when cancelling an already cancelled booking")
    void shouldBeIdempotentWhenCancellingAlreadyCancelledBooking() {
        testBooking.cancel(NOW);
        UUID publicId = testBooking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancel(publicId);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).saveAndFlush(testBooking);
    }

    @Test
    @DisplayName("Should throw CancellationTooLateException when cancelling less than 2 hours before start")
    void shouldThrowWhenCancellingTooLate() {
        Booking lateBooking = newBooking(resourceId, NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(2)));
        UUID publicId = lateBooking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.of(lateBooking));

        assertThatThrownBy(() -> bookingService.cancel(publicId)).isInstanceOf(CancellationTooLateException.class);
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BookingAlreadyCompletedException when cancelling a completed booking")
    void shouldThrowWhenCancellingCompletedBooking() {
        testBooking.confirm();
        testBooking.complete(STARTS_AT);
        UUID publicId = testBooking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.cancel(publicId)).isInstanceOf(BookingAlreadyCompletedException.class);
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BookingNotFoundException when cancelling a missing booking")
    void shouldThrowWhenCancellingMissingBooking() {
        UUID publicId = testBooking.getPublicId();
        when(bookingRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel(publicId)).isInstanceOf(BookingNotFoundException.class);
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Should return the whole day as free when there are no bookings")
    void shouldReturnWholeDayWhenThereAreNoBookings() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        Instant dayStart = Instant.parse("2026-09-02T00:00:00Z");
        Instant dayEnd = Instant.parse("2026-09-03T00:00:00Z");
        when(bookingRepository.findAll(anyBookingSpec(), any(Sort.class))).thenReturn(List.of());

        AvailableSlotsResponse response = bookingService.findAvailableSlots(resourceId, date);

        assertThat(response.resourceId()).isEqualTo(resourceId);
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.slots()).containsExactly(new AvailableSlotsResponse.TimeSlot(dayStart, dayEnd));
        verify(resourceService).requireActive(resourceId);
    }

    @Test
    @DisplayName("Should split free slots around a busy booking")
    void shouldSplitFreeSlotsAroundBusyBooking() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        Instant busyStart = Instant.parse("2026-09-02T10:00:00Z");
        Instant busyEnd = Instant.parse("2026-09-02T11:00:00Z");
        Booking busy = newBooking(resourceId, busyStart, busyEnd);
        when(bookingRepository.findAll(anyBookingSpec(), any(Sort.class))).thenReturn(List.of(busy));

        AvailableSlotsResponse response = bookingService.findAvailableSlots(resourceId, date);

        assertThat(response.slots())
                .containsExactly(
                        new AvailableSlotsResponse.TimeSlot(Instant.parse("2026-09-02T00:00:00Z"), busyStart),
                        new AvailableSlotsResponse.TimeSlot(busyEnd, Instant.parse("2026-09-03T00:00:00Z")));
    }

    @Test
    @DisplayName("Should not emit a gap between adjacent bookings")
    void shouldNotEmitGapBetweenAdjacentBookings() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        Instant firstStart = Instant.parse("2026-09-02T10:00:00Z");
        Instant firstEnd = Instant.parse("2026-09-02T11:00:00Z");
        Instant secondEnd = Instant.parse("2026-09-02T12:00:00Z");
        when(bookingRepository.findAll(anyBookingSpec(), any(Sort.class)))
                .thenReturn(List.of(
                        newBooking(resourceId, firstStart, firstEnd), newBooking(resourceId, firstEnd, secondEnd)));

        AvailableSlotsResponse response = bookingService.findAvailableSlots(resourceId, date);

        assertThat(response.slots())
                .containsExactly(
                        new AvailableSlotsResponse.TimeSlot(Instant.parse("2026-09-02T00:00:00Z"), firstStart),
                        new AvailableSlotsResponse.TimeSlot(secondEnd, Instant.parse("2026-09-03T00:00:00Z")));
    }

    @Test
    @DisplayName("Should start available slots from now when the requested date is today")
    void shouldStartAvailableSlotsFromNowWhenDateIsToday() {
        LocalDate today = LocalDate.of(2026, 9, 1);
        when(bookingRepository.findAll(anyBookingSpec(), any(Sort.class))).thenReturn(List.of());

        AvailableSlotsResponse response = bookingService.findAvailableSlots(resourceId, today);

        assertThat(response.slots())
                .containsExactly(new AvailableSlotsResponse.TimeSlot(NOW, Instant.parse("2026-09-02T00:00:00Z")));
    }

    @Test
    @DisplayName("Should return no slots when the requested date is already in the past")
    void shouldReturnNoSlotsWhenDateIsInThePast() {
        LocalDate past = LocalDate.of(2026, 8, 31);
        when(bookingRepository.findAll(anyBookingSpec(), any(Sort.class))).thenReturn(List.of());

        AvailableSlotsResponse response = bookingService.findAvailableSlots(resourceId, past);

        assertThat(response.slots()).isEmpty();
    }

    @Test
    @DisplayName("Should return no trailing slot when cursor reaches day end")
    void shouldNotAddTrailingSlotWhenCursorEqualsDayEnd() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        Instant busyStart = Instant.parse("2026-09-02T23:00:00Z");
        Instant dayEnd = Instant.parse("2026-09-03T00:00:00Z");
        Booking busy = newBooking(resourceId, busyStart, dayEnd);
        when(bookingRepository.findAll(anyBookingSpec(), any(Sort.class))).thenReturn(List.of(busy));

        AvailableSlotsResponse response = bookingService.findAvailableSlots(resourceId, date);

        assertThat(response.slots())
                .containsExactly(new AvailableSlotsResponse.TimeSlot(Instant.parse("2026-09-02T00:00:00Z"), busyStart));
    }

    @Test
    @DisplayName("Should throw when finding available slots for a missing resource")
    void shouldThrowWhenFindingAvailableSlotsForMissingResource() {
        doThrow(new ResourceNotFoundException(resourceId)).when(resourceService).requireActive(resourceId);

        assertThatThrownBy(() -> bookingService.findAvailableSlots(resourceId, LocalDate.of(2026, 9, 2)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bookingRepository, never()).findAll(anyBookingSpec(), any(Sort.class));
    }

    @SuppressWarnings("unchecked")
    private static Specification<Booking> anyBookingSpec() {
        return any(Specification.class);
    }

    private Booking bookingInStatus(BookingStatus status) {
        Booking booking = newBooking(resourceId, STARTS_AT, ENDS_AT);
        switch (status) {
            case PENDING -> {}
            case CONFIRMED -> booking.confirm();
            case CANCELLED -> booking.cancel(NOW);
            case COMPLETED -> {
                booking.confirm();
                booking.complete(STARTS_AT);
            }
        }
        return booking;
    }

    private static Booking newBooking(UUID resourceId, Instant startsAt, Instant endsAt) {
        return new Booking(
                resourceId,
                new CustomerDetails(CUSTOMER_EMAIL, CUSTOMER_NAME),
                new BookingInterval(startsAt, endsAt),
                NOW,
                new BookingPricing(BigDecimal.valueOf(100.00), "USD"));
    }
}
