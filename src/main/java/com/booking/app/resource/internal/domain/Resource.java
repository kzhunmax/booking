package com.booking.app.resource.internal.domain;

import com.booking.app.common.AuditInfo;
import com.booking.app.common.Identifiable;
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
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "resources")
@EntityListeners(AuditingEntityListener.class)
public class Resource implements Identifiable {

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

    protected Resource() {}

    public Resource(String name, String description) {
        validateName(name);
        this.publicId = UUID.randomUUID();
        this.auditInfo = new AuditInfo();
        this.name = name.strip();
        this.description = description;
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

    public void rename(String name) {
        validateName(name);
        verifyArchivedStatus();
        this.name = name.strip();
    }

    public String getDescription() {
        return description;
    }

    public void changeDescription(String description) {
        verifyArchivedStatus();
        this.description = description;
    }

    public ResourceStatus getStatus() {
        return status;
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

    private void validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        String trimmed = name.strip();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Name cannot exceed 255 characters");
        }
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
