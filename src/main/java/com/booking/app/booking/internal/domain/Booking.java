package com.booking.app.booking.internal.domain;

import com.booking.app.booking.BookingAlreadyCompletedException;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.CancellationTooLateException;
import com.booking.app.booking.InvalidStatusTransitionException;
import com.booking.app.common.AuditInfo;
import com.booking.app.common.Identifiable;
import com.booking.app.common.Require;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "bookings")
@EntityListeners(AuditingEntityListener.class)
public class Booking implements Identifiable {

    private static final Duration CANCELLATION_DEADLINE = Duration.ofHours(2);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bookings_seq")
    @SequenceGenerator(name = "bookings_seq", sequenceName = "bookings_seq", allocationSize = 50)
    private Long id;

    @Column(name = "public_id", unique = true, updatable = false, nullable = false)
    private UUID publicId;

    @Version
    private Long version;

    @Column(name = "resource_id", nullable = false)
    private UUID resourcePublicId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Embedded
    private AuditInfo auditInfo;

    protected Booking() {}

    public Booking(UUID resourcePublicId, CustomerDetails customer, BookingInterval interval, Instant now) {
        Require.notNull(resourcePublicId, "resourcePublicId cannot be null");
        Require.notNull(customer, "customer cannot be null");
        Require.notNull(interval, "interval cannot be null");
        Require.notNull(now, "now cannot be null");
        Require.argument(interval.startsAt().isAfter(now), "startsAt must be in the future");

        this.publicId = UUID.randomUUID();
        this.auditInfo = new AuditInfo();
        this.resourcePublicId = resourcePublicId;
        this.customerEmail = customer.email();
        this.customerName = customer.name();
        this.startsAt = interval.startsAt();
        this.endsAt = interval.endsAt();
        this.status = BookingStatus.PENDING;
    }

    @Override
    public UUID getPublicId() {
        return publicId;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getResourcePublicId() {
        return resourcePublicId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    public void confirm() {
        if (this.status == BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Cannot confirm a cancelled booking");
        }
        if (this.status == BookingStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("Cannot confirm a completed booking");
        }
        if (this.status == BookingStatus.CONFIRMED) {
            return;
        }
        this.status = BookingStatus.CONFIRMED;
    }

    public void complete(Instant now) {
        Require.notNull(now, "now cannot be null");
        if (this.status == BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Cannot complete a cancelled booking");
        }
        if (this.status == BookingStatus.PENDING) {
            throw new InvalidStatusTransitionException("Cannot complete a booking that has not been confirmed");
        }
        if (this.status == BookingStatus.COMPLETED) {
            return;
        }
        if (now.isBefore(startsAt)) {
            throw new InvalidStatusTransitionException("Cannot complete a booking that has not started");
        }
        this.status = BookingStatus.COMPLETED;
    }

    public void cancel(Instant now) {
        Require.notNull(now, "now cannot be null");
        if (this.status == BookingStatus.CANCELLED) {
            return;
        }
        if (this.status == BookingStatus.COMPLETED) {
            throw new BookingAlreadyCompletedException("Cannot cancel a booking that has already been completed");
        }
        if (!now.isBefore(startsAt)) {
            throw new CancellationTooLateException("Cannot cancel a booking that has already started");
        }
        if (now.plus(CANCELLATION_DEADLINE).isAfter(startsAt)) {
            throw new CancellationTooLateException("Cannot cancel later than 2 hours before start");
        }
        this.status = BookingStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking that)) return false;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }
}
