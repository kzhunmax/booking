package com.booking.app.booking.internal.domain;

import com.booking.app.booking.BookingAlreadyCompletedException;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.CancellationTooLateException;
import com.booking.app.common.AuditInfo;
import com.booking.app.common.Identifiable;
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

    public Booking(UUID resourcePublicId, String customerEmail, String customerName, Instant startsAt, Instant endsAt) {
        validateTimestamps(startsAt, endsAt);
        if (resourcePublicId == null) throw new IllegalArgumentException("resourceId cannot be null");
        if (customerEmail == null || customerEmail.isBlank())
            throw new IllegalArgumentException("customerEmail cannot be blank");
        if (customerName == null || customerName.isBlank())
            throw new IllegalArgumentException("customerName cannot be blank");

        this.publicId = UUID.randomUUID();
        this.auditInfo = new AuditInfo();
        this.resourcePublicId = resourcePublicId;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = BookingStatus.PENDING;
    }

    @Override
    public UUID getPublicId() {
        return publicId;
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

    private void validateTimestamps(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("Interval values cannot be null");
        }
        if (endsAt.isBefore(startsAt) || endsAt.equals(startsAt)) {
            throw new IllegalArgumentException("ends_at must be after starts_at");
        }
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");
        if (this.status == BookingStatus.CANCELLED) return;
        if (this.status == BookingStatus.COMPLETED) {
            throw new BookingAlreadyCompletedException("Cannot cancel a booking that has already been completed");
        }
        if (now.plus(CANCELLATION_DEADLINE).isAfter(startsAt)) {
            throw new CancellationTooLateException("You can't cancel later than 2 hours before starting date");
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
