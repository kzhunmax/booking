package com.booking.app.notification.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booking.app.notification.InvalidStatusTransitionException;
import com.booking.app.notification.NotificationStatus;
import com.booking.app.notification.NotificationType;
import java.time.Instant;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NotificationTest {

    private NotificationType type;
    private String recipient;
    private String subject;
    private String body;
    private Instant now;

    @BeforeEach
    void setUp() {
        type = NotificationType.BOOKING_CONFIRMED;
        recipient = "john@example.com";
        subject = "Booking Confirmed";
        body = "Your booking was successfully confirmed.";
        now = Instant.parse("2026-09-03T10:15:30Z");
    }

    private Notification createValidNotification() {
        return new Notification(type, recipient, subject, body);
    }

    @Nested
    @DisplayName("Notification Creation")
    class Creation {

        @Test
        @DisplayName("Should create notification with valid fields in PENDING status")
        void shouldCreateNotificationWithValidFields() {
            Notification notification = createValidNotification();

            assertThat(notification.getPublicId()).isNotNull();
            assertThat(notification.getType()).isEqualTo(type);
            assertThat(notification.getRecipient()).isEqualTo(recipient);
            assertThat(notification.getSubject()).isEqualTo(subject);
            assertThat(notification.getBody()).isEqualTo(body);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(notification.getSentAt()).isNull();
            assertThat(notification.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when type is null")
        void shouldThrowExceptionWhenTypeIsNull() {
            assertThatThrownBy(() -> new Notification(null, recipient, subject, body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("type cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when recipient is null")
        void shouldThrowExceptionWhenRecipientIsNull() {
            assertThatThrownBy(() -> new Notification(type, null, subject, body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("recipient cannot be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw exception when recipient is blank")
        void shouldThrowExceptionWhenRecipientIsBlank(String blankRecipient) {
            assertThatThrownBy(() -> new Notification(type, blankRecipient, subject, body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("notification must have at least one recipient");
        }

        @Test
        @DisplayName("Should throw exception when subject is null")
        void shouldThrowExceptionWhenSubjectIsNull() {
            assertThatThrownBy(() -> new Notification(type, recipient, null, body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("subject cannot be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw exception when subject is blank")
        void shouldThrowExceptionWhenSubjectIsBlank(String blankSubject) {
            assertThatThrownBy(() -> new Notification(type, recipient, blankSubject, body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("notification must have subject");
        }

        @Test
        @DisplayName("Should throw exception when body is null")
        void shouldThrowExceptionWhenBodyIsNull() {
            assertThatThrownBy(() -> new Notification(type, recipient, subject, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("body cannot be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw exception when body is blank")
        void shouldThrowExceptionWhenBodyIsBlank(String blankBody) {
            assertThatThrownBy(() -> new Notification(type, recipient, subject, blankBody))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("notification must have body");
        }
    }

    @Nested
    @DisplayName("Status Transitions (PENDING -> SENT / FAILED)")
    class StatusTransitions {

        @Test
        @DisplayName("Should mark notification as SENT with sentAt timestamp")
        void shouldMarkAsSent() {
            Notification notification = createValidNotification();
            notification.markAsSent(now);

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(notification.getSentAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should be idempotent when marking as SENT multiple times")
        void shouldBeIdempotentWhenMarkingAsSent() {
            Notification notification = createValidNotification();
            notification.markAsSent(now);
            notification.markAsSent(now.plusSeconds(60));

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(notification.getSentAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should throw exception when markAsSent has null sentAt")
        void shouldThrowWhenSentAtIsNull() {
            Notification notification = createValidNotification();

            assertThatThrownBy(() -> notification.markAsSent(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("sentAt cannot be null");
        }

        @Test
        @DisplayName("Should not allow markAsSent on FAILED notification")
        void shouldNotAllowMarkAsSentOnFailedNotification() {
            Notification notification = createValidNotification();
            notification.markAsFailed();

            assertThatThrownBy(() -> notification.markAsSent(now))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot send failed notification");
        }

        @Test
        @DisplayName("Should mark notification as FAILED")
        void shouldMarkAsFailed() {
            Notification notification = createValidNotification();
            notification.markAsFailed();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(notification.getSentAt()).isNull();
        }

        @Test
        @DisplayName("Should be idempotent when marking as FAILED multiple times")
        void shouldBeIdempotentWhenMarkingAsFailed() {
            Notification notification = createValidNotification();
            notification.markAsFailed();
            notification.markAsFailed();

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        }

        @Test
        @DisplayName("Should not allow markAsFailed on SENT notification")
        void shouldNotAllowMarkAsFailedOnSentNotification() {
            Notification notification = createValidNotification();
            notification.markAsSent(now);

            assertThatThrownBy(notification::markAsFailed)
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Cannot fail already sent notification");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class Equality {

        @Test
        @DisplayName("Should verify equals and hashCode contract")
        void shouldVerifyEqualsAndHashCode() {
            EqualsVerifier.forClass(Notification.class)
                    .withOnlyTheseFields("publicId")
                    .suppress(Warning.NONFINAL_FIELDS)
                    .verify();
        }
    }
}
