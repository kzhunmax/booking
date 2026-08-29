package com.booking.app.resource.internal.domain;

import com.booking.app.common.AuditInfo;
import com.booking.app.common.Identifiable;
import com.booking.app.common.Require;
import com.booking.app.resource.InvalidStatusTransitionException;
import com.booking.app.resource.ResourceStatus;
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
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "resources")
@EntityListeners(AuditingEntityListener.class)
public class Resource implements Identifiable {

    private static final int CURRENCY_CODE_LENGTH = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resources_seq")
    @SequenceGenerator(name = "resources_seq", sequenceName = "resources_seq", allocationSize = 50)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Version
    private Long version;

    @Embedded
    private AuditInfo auditInfo;

    @Column(nullable = false)
    private String name;

    @Column(length = 10000)
    private String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status = ResourceStatus.ACTIVE;

    @Column(name = "price_per_hour", nullable = false, precision = 19, scale = 4)
    private BigDecimal pricePerHour;

    @Column(name = "price_currency", nullable = false, length = CURRENCY_CODE_LENGTH)
    private String currency;

    protected Resource() {}

    public Resource(ResourceDetails details, ResourcePricing pricing) {
        Require.notNull(details, "details cannot be null");
        Require.notNull(pricing, "pricing cannot be null");

        this.publicId = UUID.randomUUID();
        this.auditInfo = new AuditInfo();
        this.name = details.name();
        this.description = details.description();
        this.pricePerHour = pricing.pricePerHour();
        this.currency = pricing.currency().strip().toUpperCase();
    }

    public Long getId() {
        return id;
    }

    @Override
    public UUID getPublicId() {
        return publicId;
    }

    public Long getVersion() {
        return version;
    }

    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    public String getName() {
        return name;
    }

    public void updateDetails(ResourceDetails details) {
        verifyArchivedStatus();
        Require.notNull(details, "details cannot be null");
        this.name = details.name();
        this.description = details.description();
    }

    public String getDescription() {
        return description;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public String getCurrency() {
        return currency;
    }

    public void activate() {
        verifyArchivedStatus();
        this.status = ResourceStatus.ACTIVE;
    }

    public void deactivate() {
        verifyArchivedStatus();
        this.status = ResourceStatus.INACTIVE;
    }

    private void verifyArchivedStatus() {
        if (this.status == ResourceStatus.ARCHIVED) {
            throw new InvalidStatusTransitionException("Archived resources cannot be changed");
        }
    }

    public void archive() {
        if (this.status == ResourceStatus.ARCHIVED) {
            return;
        }
        this.status = ResourceStatus.ARCHIVED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource that)) return false;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
