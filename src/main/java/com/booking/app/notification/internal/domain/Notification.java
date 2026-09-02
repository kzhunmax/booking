package com.booking.app.notification.internal.domain;

import com.booking.app.common.Identifiable;
import com.booking.app.common.Require;
import com.booking.app.notification.InvalidStatusTransitionException;
import com.booking.app.notification.NotificationStatus;
import com.booking.app.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq")
    @SequenceGenerator(name = "notifications_seq", sequenceName = "notifications_seq", allocationSize = 50)
    private Long id;

    @Column(name = "public_id", unique = true, updatable = false, nullable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "recipient", nullable = false, updatable = false)
    private String recipient;

    @Column(name = "subject", nullable = false, updatable = false)
    private String subject;

    @Column(name = "body", nullable = false, updatable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {}

    public Notification(NotificationType type, String recipient, String subject, String body) {
        Require.notNull(type, "type cannot be null");
        Require.notNull(recipient, "recipient cannot be null");
        Require.notNull(subject, "subject cannot be null");
        Require.notNull(body, "body cannot be null");
        Require.argument(!recipient.isBlank(), "notification must have at least one recipient");
        Require.argument(!subject.isBlank(), "notification must have subject");
        Require.argument(!body.isBlank(), "notification must have body");

        this.publicId = UUID.randomUUID();
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.type = type;
        this.status = NotificationStatus.PENDING;
    }

    public void markAsSent(Instant sentAt) {
        Require.notNull(sentAt, "sentAt cannot be null");
        if (this.status == NotificationStatus.FAILED) {
            throw new InvalidStatusTransitionException("Cannot send failed notification");
        }
        if (this.status == NotificationStatus.SENT) {
            return;
        }
        this.status = NotificationStatus.SENT;
        this.sentAt = sentAt;
    }

    public void markAsFailed() {
        if (this.status == NotificationStatus.SENT) {
            throw new InvalidStatusTransitionException("Cannot fail already sent notification");
        }
        if (this.status == NotificationStatus.FAILED) {
            return;
        }
        this.status = NotificationStatus.FAILED;
    }

    @Override
    public UUID getPublicId() {
        return publicId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }
}
