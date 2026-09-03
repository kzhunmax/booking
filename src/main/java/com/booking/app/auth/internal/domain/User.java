package com.booking.app.auth.internal.domain;

import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
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
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 50)
    private Long id;

    @Column(name = "public_id", unique = true, updatable = false, nullable = false)
    private UUID publicId;

    @Version
    private Long version;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Embedded
    private AuditInfo auditInfo;

    protected User() {}

    public User(String email, String passwordHash, String name, UserRole role) {
        Require.notNull(email, "email cannot be null");
        Require.argument(!email.isBlank(), "email cannot be empty");
        Require.notNull(passwordHash, "passwordHash cannot be null");
        Require.argument(!passwordHash.isBlank(), "passwordHash cannot be empty");
        Require.notNull(name, "name cannot be null");
        Require.argument(!name.isBlank(), "name cannot be empty");
        Require.notNull(role, "role cannot be null");

        this.publicId = UUID.randomUUID();
        this.auditInfo = new AuditInfo();
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    @Override
    public UUID getPublicId() {
        return publicId;
    }

    public Long getVersion() {
        return version;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return auditInfo.getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return auditInfo.getUpdatedAt();
    }

    public void block() {
        if (this.status == UserStatus.BLOCKED) {
            return;
        }
        this.status = UserStatus.BLOCKED;
    }

    public void unblock() {
        if (this.status == UserStatus.ACTIVE) {
            return;
        }
        this.status = UserStatus.ACTIVE;
    }
}
