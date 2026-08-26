package com.booking.app.booking.internal.domain;

import com.booking.app.booking.BookingStatus;
import com.booking.app.common.AuditInfo;
import com.booking.app.common.Identifiable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Booking implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
    @SequenceGenerator(name = "booking_seq", sequenceName = "booking_seq", allocationSize = 50)
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
    @Column(name = "status")
    private BookingStatus status;

    @Embedded
    private AuditInfo auditInfo;

    protected Booking() {}

    public Booking(UUID resourcePublicId, String customerEmail, String customerName, Instant startsAt, Instant endsAt) {
        validateTimestamps(startsAt, endsAt);
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

    private void validateTimestamps(Instant startsAt, Instant endsAt) {
        if (endsAt.isBefore(startsAt) || endsAt.equals(startsAt)) {
            throw new IllegalArgumentException("ends_at must be after starts_at");
        }
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
