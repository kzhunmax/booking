package com.booking.app.booking.internal.infrastructure;

import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.internal.domain.Booking;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BookingSpecifications {

    private BookingSpecifications() {}

    public static Specification<Booking> forResource(UUID resourceId) {
        return (root, query, cb) -> resourceId == null ? null : cb.equal(root.get("resourcePublicId"), resourceId);
    }

    public static Specification<Booking> withStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> notCancelled() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), BookingStatus.CANCELLED);
    }

    public static Specification<Booking> endsAfter(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThan(root.get("endsAt"), from);
    }

    public static Specification<Booking> startsBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("startsAt"), to);
    }

    public static Specification<Booking> overlapping(Instant from, Instant to) {
        return Specification.where(endsAfter(from)).and(startsBefore(to));
    }

    public static Specification<Booking> filter(UUID resourceId, BookingStatus status, Instant from, Instant to) {
        return Specification.where(forResource(resourceId))
                .and(withStatus(status))
                .and(overlapping(from, to));
    }

    public static Specification<Booking> activeBookingsInInterval(UUID resourceId, Instant from, Instant to) {
        return Specification.where(forResource(resourceId)).and(notCancelled()).and(overlapping(from, to));
    }
}
