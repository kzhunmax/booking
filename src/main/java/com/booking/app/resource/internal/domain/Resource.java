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
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "resources")
@EntityListeners(AuditingEntityListener.class)
public class Resource implements Identifiable {

    private static final int NAME_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 10000;

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
        this.publicId = UUID.randomUUID();
        this.auditInfo = new AuditInfo();
        this.name = validateName(name);
        this.description = normalizeDescription(description);
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
        verifyArchivedStatus();
        this.name = validateName(name);
    }

    public String getDescription() {
        return description;
    }

    public void changeDescription(String description) {
        verifyArchivedStatus();
        this.description = normalizeDescription(description);
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

    private String validateName(String name) {
        Require.notNull(name, "Name cannot be null");
        String trimmed = name.strip();
        Require.argument(!trimmed.isBlank(), "Name cannot be blank");
        Require.argument(trimmed.length() <= NAME_MAX_LENGTH, "Name cannot exceed 255 characters");
        return trimmed;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        Require.argument(trimmed.length() <= DESCRIPTION_MAX_LENGTH, "Description cannot exceed 10000 characters");
        return trimmed;
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
