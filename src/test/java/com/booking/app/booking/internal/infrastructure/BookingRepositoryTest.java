package com.booking.app.booking.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.internal.domain.Booking;
import com.booking.app.booking.internal.domain.BookingInterval;
import com.booking.app.booking.internal.domain.BookingPricing;
import com.booking.app.booking.internal.domain.CustomerDetails;
import com.booking.app.config.JpaConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class BookingRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-09-01T10:00:00Z");
    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.valueOf(100.0);
    private static final String DEFAULT_CURRENCY = "USD";

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("Should save and find booking by publicId")
    void shouldSaveAndFindByPublicId() {
        UUID resourceId = UUID.randomUUID();
        Booking booking = booking(resourceId, BASE.plus(Duration.ofHours(4)), BASE.plus(Duration.ofHours(5)));

        bookingRepository.saveAndFlush(booking);
        Optional<Booking> found = bookingRepository.findByPublicId(booking.getPublicId());

        assertThat(found).isPresent();
        assertThat(found.get().getResourcePublicId()).isEqualTo(resourceId);
        assertThat(found.get().getCustomerEmail()).isEqualTo("customer@example.com");
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(found.get().getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should persist booking with generated fields")
    void shouldPersistBookingWithGeneratedFields() {
        Booking booking = booking(UUID.randomUUID(), BASE.plus(Duration.ofHours(4)), BASE.plus(Duration.ofHours(5)));

        Booking saved = bookingRepository.saveAndFlush(booking);

        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getAuditInfo().getCreatedAt()).isNotNull();
        assertThat(saved.getAuditInfo().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should filter bookings by resource and overlapping interval")
    void shouldFilterBookingsByResourceAndOverlappingInterval() {
        UUID roomA = UUID.randomUUID();
        UUID roomB = UUID.randomUUID();
        Instant start = BASE.plus(Duration.ofHours(4));
        Instant end = BASE.plus(Duration.ofHours(5));
        Booking matching = booking(roomA, start, end);
        Booking otherResource = booking(roomB, start, end);
        Booking outsideWindow = booking(roomA, BASE.plus(Duration.ofHours(8)), BASE.plus(Duration.ofHours(9)));
        bookingRepository.saveAndFlush(matching);
        bookingRepository.saveAndFlush(otherResource);
        bookingRepository.saveAndFlush(outsideWindow);

        Page<Booking> page = bookingRepository.findAll(
                BookingSpecifications.filter(roomA, BookingStatus.PENDING, start, end), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isOne();
        assertThat(page.getContent().getFirst().getPublicId()).isEqualTo(matching.getPublicId());
    }

    @Test
    @DisplayName("Should exclude cancelled bookings from active interval query")
    void shouldExcludeCancelledBookingsFromActiveIntervalQuery() {
        UUID resourceId = UUID.randomUUID();
        Instant start = BASE.plus(Duration.ofHours(4));
        Instant end = BASE.plus(Duration.ofHours(5));
        Booking active = booking(resourceId, start, end);
        Booking cancelled = booking(resourceId, BASE.plus(Duration.ofHours(6)), BASE.plus(Duration.ofHours(7)));
        cancelled.cancel(BASE);
        bookingRepository.saveAndFlush(active);
        bookingRepository.saveAndFlush(cancelled);

        List<Booking> found = bookingRepository.findAll(
                BookingSpecifications.activeBookingsInInterval(
                        resourceId, BASE.plus(Duration.ofHours(3)), BASE.plus(Duration.ofHours(8))),
                Sort.by(Sort.Direction.ASC, "startsAt"));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getPublicId()).isEqualTo(active.getPublicId());
    }

    @Test
    @DisplayName("Should reject overlapping bookings for the same resource")
    void shouldRejectOverlappingBookingsForTheSameResource() {
        UUID resourceId = UUID.randomUUID();
        Instant start = BASE.plus(Duration.ofHours(4));
        Instant end = BASE.plus(Duration.ofHours(6));
        bookingRepository.saveAndFlush(booking(resourceId, start, end));

        Booking overlapping = booking(resourceId, start.plus(Duration.ofHours(1)), end.plus(Duration.ofHours(1)));

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(overlapping))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should allow adjacent back-to-back bookings for the same resource")
    void shouldAllowAdjacentBookingsForTheSameResource() {
        UUID resourceId = UUID.randomUUID();
        Instant firstStart = BASE.plus(Duration.ofHours(4));
        Instant firstEnd = BASE.plus(Duration.ofHours(5));
        bookingRepository.saveAndFlush(booking(resourceId, firstStart, firstEnd));

        Booking adjacent = booking(resourceId, firstEnd, firstEnd.plus(Duration.ofHours(1)));
        Booking saved = bookingRepository.saveAndFlush(adjacent);

        assertThat(saved.getPublicId()).isNotNull();
        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should allow overlapping interval after the original booking is cancelled")
    void shouldAllowRebookingAfterCancellation() {
        UUID resourceId = UUID.randomUUID();
        Instant start = BASE.plus(Duration.ofHours(4));
        Instant end = BASE.plus(Duration.ofHours(5));
        Booking original = booking(resourceId, start, end);
        original.cancel(BASE);
        bookingRepository.saveAndFlush(original);

        Booking replacement = booking(resourceId, start, end);
        Booking saved = bookingRepository.saveAndFlush(replacement);

        assertThat(saved.getPublicId()).isNotEqualTo(original.getPublicId());
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    private static Booking booking(UUID resourceId, Instant startsAt, Instant endsAt) {
        Instant now = startsAt.minus(Duration.ofHours(4));
        return new Booking(
                resourceId,
                new CustomerDetails("customer@example.com", "John Doe"),
                new BookingInterval(startsAt, endsAt),
                now,
                new BookingPricing(DEFAULT_AMOUNT, DEFAULT_CURRENCY));
    }
}
