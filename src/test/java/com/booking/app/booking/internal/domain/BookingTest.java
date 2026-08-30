package com.booking.app.booking.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booking.app.booking.BookingAlreadyCompletedException;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.CancellationTooLateException;
import com.booking.app.booking.InvalidStatusTransitionException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final Instant BASE_TIME = Instant.parse("2026-09-01T10:00:00Z");

    private UUID resourcePublicId;
    private CustomerDetails customer;
    private BookingInterval interval;
    private BookingPricing pricing;
    private Instant startsAt;
    private Instant endsAt;

    @BeforeEach
    void setUp() {
        resourcePublicId = UUID.randomUUID();
        startsAt = BASE_TIME.plus(Duration.ofHours(4)); // 14:00
        endsAt = BASE_TIME.plus(Duration.ofHours(5)); // 15:00
        customer = new CustomerDetails("customer@example.com", "John Doe");
        interval = new BookingInterval(startsAt, endsAt);
        pricing = new BookingPricing(BigDecimal.valueOf(100.00), "USD");
    }

    private Booking createValidBooking() {
        return new Booking(resourcePublicId, customer, interval, BASE_TIME, pricing);
    }

    @Nested
    @DisplayName("Booking Creation")
    class Creation {

        @Test
        @DisplayName("Should create booking with valid fields in PENDING status")
        void shouldCreateBookingWithValidFields() {
            Booking booking = createValidBooking();

            assertThat(booking.getPublicId()).isNotNull();
            assertThat(booking.getResourcePublicId()).isEqualTo(resourcePublicId);
            assertThat(booking.getCustomerEmail()).isEqualTo("customer@example.com");
            assertThat(booking.getCustomerName()).isEqualTo("John Doe");
            assertThat(booking.getStartsAt()).isEqualTo(startsAt);
            assertThat(booking.getEndsAt()).isEqualTo(endsAt);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(booking.getTotalAmount()).isEqualTo(BigDecimal.valueOf(100.00));
            assertThat(booking.getCurrency()).isEqualTo("USD");
            assertThat(booking.getAuditInfo()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when resourcePublicId is null")
        void shouldThrowExceptionForNullResourcePublicId() {
            assertThatThrownBy(() -> new Booking(null, customer, interval, BASE_TIME, pricing))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("resourcePublicId cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when customer is null")
        void shouldThrowExceptionForNullCustomer() {
            assertThatThrownBy(() -> new Booking(resourcePublicId, null, interval, BASE_TIME, pricing))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("customer cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when interval is null")
        void shouldThrowExceptionForNullInterval() {
            assertThatThrownBy(() -> new Booking(resourcePublicId, customer, null, BASE_TIME, pricing))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("interval cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when now is null")
        void shouldThrowExceptionForNullNow() {
            assertThatThrownBy(() -> new Booking(resourcePublicId, customer, interval, null, pricing))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("now cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when pricing is null")
        void shouldThrowExceptionForNullPricing() {
            assertThatThrownBy(() -> new Booking(resourcePublicId, customer, interval, BASE_TIME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("pricing cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when startsAt is not in the future")
        void shouldThrowExceptionWhenStartsAtIsNotInTheFuture() {
            BookingInterval pastInterval =
                    new BookingInterval(BASE_TIME.minus(Duration.ofHours(2)), BASE_TIME.minus(Duration.ofHours(1)));

            assertThatThrownBy(() -> new Booking(resourcePublicId, customer, pastInterval, BASE_TIME, pricing))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("startsAt must be in the future");
        }
    }

    @Nested
    @DisplayName("Status Workflow Transitions (PENDING -> CONFIRMED -> COMPLETED)")
    class StatusTransitions {

        @Test
        @DisplayName("Should confirm pending booking (PENDING -> CONFIRMED)")
        void shouldConfirmPendingBooking() {
            Booking booking = createValidBooking();

            booking.confirm();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should be idempotent when confirming already confirmed booking")
        void shouldBeIdempotentWhenConfirmingAlreadyConfirmedBooking() {
            Booking booking = createValidBooking();
            booking.confirm();

            booking.confirm();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should complete confirmed booking (CONFIRMED -> COMPLETED)")
        void shouldCompleteConfirmedBooking() {
            Booking booking = createValidBooking();
            booking.confirm();

            booking.complete(startsAt);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should be idempotent when completing already completed booking")
        void shouldBeIdempotentWhenCompletingAlreadyCompletedBooking() {
            Booking booking = createValidBooking();
            booking.confirm();
            booking.complete(startsAt);

            booking.complete(startsAt);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should not complete pending booking without confirmation")
        void shouldNotCompletePendingBooking() {
            Booking booking = createValidBooking();

            assertThatThrownBy(() -> booking.complete(startsAt))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot complete a booking that has not been confirmed");
        }

        @Test
        @DisplayName("Should not complete confirmed booking before it has started")
        void shouldNotCompleteBookingBeforeStart() {
            Booking booking = createValidBooking();
            booking.confirm();

            assertThatThrownBy(() -> booking.complete(startsAt.minus(Duration.ofMinutes(1))))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot complete a booking that has not started");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when complete timestamp is null")
        void shouldThrowExceptionWhenCompleteTimestampIsNull() {
            Booking booking = createValidBooking();
            booking.confirm();

            assertThatThrownBy(() -> booking.complete(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("now cannot be null");
        }

        @Test
        @DisplayName("Should not confirm cancelled booking")
        void shouldNotConfirmCancelledBooking() {
            Booking booking = createValidBooking();
            booking.cancel(startsAt.minus(Duration.ofHours(3)));

            assertThatThrownBy(booking::confirm)
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot confirm a cancelled booking");
        }

        @Test
        @DisplayName("Should not confirm completed booking")
        void shouldNotConfirmCompletedBooking() {
            Booking booking = createValidBooking();
            booking.confirm();
            booking.complete(startsAt);

            assertThatThrownBy(booking::confirm)
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot confirm a completed booking");
        }

        @Test
        @DisplayName("Should not complete cancelled booking")
        void shouldNotCompleteCancelledBooking() {
            Booking booking = createValidBooking();
            booking.cancel(startsAt.minus(Duration.ofHours(3)));

            assertThatThrownBy(() -> booking.complete(startsAt))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot complete a cancelled booking");
        }
    }

    @Nested
    @DisplayName("Cancellation Workflow (PENDING | CONFIRMED -> CANCELLED)")
    class CancellationPolicy {

        @Test
        @DisplayName("Should allow cancellation of PENDING booking more than 2 hours in advance")
        void shouldAllowCancellationOfPendingBooking() {
            Booking booking = createValidBooking();
            Instant cancelTime = startsAt.minus(Duration.ofHours(2)).minus(Duration.ofMinutes(1)); // 11:59 for 14:00

            booking.cancel(cancelTime);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should allow cancellation of CONFIRMED booking more than 2 hours in advance")
        void shouldAllowCancellationOfConfirmedBooking() {
            Booking booking = createValidBooking();
            booking.confirm();
            Instant cancelTime = startsAt.minus(Duration.ofHours(3));

            booking.cancel(cancelTime);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should allow cancellation exactly 2 hours in advance (boundary check)")
        void shouldAllowCancellationExactlyTwoHoursInAdvance() {
            Booking booking = createValidBooking();
            Instant exactDeadline = startsAt.minus(Duration.ofHours(2)); // 12:00 for 14:00

            booking.cancel(exactDeadline);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw CancellationTooLateException when cancelling less than 2 hours before start")
        void shouldThrowExceptionWhenCancellingLessThanTwoHoursBeforeStart() {
            Booking booking = createValidBooking();
            Instant lateTime = startsAt.minus(Duration.ofHours(2)).plus(Duration.ofMinutes(1)); // 12:01 for 14:00

            assertThatThrownBy(() -> booking.cancel(lateTime))
                    .isInstanceOf(CancellationTooLateException.class)
                    .hasMessage("Cannot cancel later than 2 hours before start");
        }

        @Test
        @DisplayName("Should throw CancellationTooLateException when cancelling after start time")
        void shouldThrowExceptionWhenCancellingAfterStartTime() {
            Booking booking = createValidBooking();
            Instant afterStart = startsAt.plus(Duration.ofMinutes(10)); // 14:10 for 14:00

            assertThatThrownBy(() -> booking.cancel(afterStart))
                    .isInstanceOf(CancellationTooLateException.class)
                    .hasMessage("Cannot cancel a booking that has already started");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when cancel timestamp is null")
        void shouldThrowExceptionWhenCancelTimestampIsNull() {
            Booking booking = createValidBooking();

            assertThatThrownBy(() -> booking.cancel(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("now cannot be null");
        }

        @Test
        @DisplayName("Should do nothing when cancelling an already cancelled booking (idempotency)")
        void shouldDoNothingWhenCancellingAlreadyCancelledBooking() {
            Booking booking = createValidBooking();
            Instant cancelTime = startsAt.minus(Duration.ofHours(3));

            booking.cancel(cancelTime);
            booking.cancel(cancelTime);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw BookingAlreadyCompletedException when cancelling a completed booking")
        void shouldThrowExceptionWhenCancellingCompletedBooking() {
            Booking booking = createValidBooking();
            booking.confirm();
            booking.complete(startsAt);

            Instant cancelTime = startsAt.minus(Duration.ofHours(3));

            assertThatThrownBy(() -> booking.cancel(cancelTime))
                    .isInstanceOf(BookingAlreadyCompletedException.class)
                    .hasMessage("Cannot cancel a booking that has already been completed");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class Equality {

        @Test
        @DisplayName("Should verify equals and hashCode contract")
        void shouldVerifyEqualsAndHashCode() {
            EqualsVerifier.forClass(Booking.class)
                    .withOnlyTheseFields("publicId")
                    .suppress(Warning.NONFINAL_FIELDS)
                    .verify();
        }
    }
}
