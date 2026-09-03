package com.booking.app.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.app.notification.internal.application.DefaultNotificationService;
import com.booking.app.notification.internal.domain.Notification;
import com.booking.app.notification.internal.infrastructure.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T12:00:00Z");
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String CUSTOMER_NAME = "John Doe";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender javaMailSender;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private NotificationService notificationService;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        notificationService = new DefaultNotificationService(
                notificationRepository, javaMailSender, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        bookingId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create, send and mark notification as SENT for booking confirmation")
    void shouldSendBookingConfirmedNotification() {
        List<NotificationStatus> savedStatuses = captureStatusesOnSave();

        notificationService.sendBookingConfirmed(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);

        verify(javaMailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@booking.com");
        assertThat(sentMessage.getTo()).containsExactly(CUSTOMER_EMAIL);
        assertThat(sentMessage.getSubject()).isEqualTo("Booking %s confirmed!".formatted(bookingId));
        assertThat(sentMessage.getText())
                .isEqualTo("Hello %s! Your booking %s is confirmed.".formatted(CUSTOMER_NAME, bookingId));

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        Notification sent = notificationCaptor.getAllValues().getLast();
        assertThat(savedStatuses).containsExactly(NotificationStatus.PENDING, NotificationStatus.SENT);
        assertThat(sent.getType()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(sent.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(sent.getSentAt()).isEqualTo(FIXED_INSTANT);
        assertThat(sent.getRecipient()).isEqualTo(CUSTOMER_EMAIL);

        InOrder inOrder = inOrder(notificationRepository, javaMailSender);
        inOrder.verify(notificationRepository).save(any(Notification.class));
        inOrder.verify(javaMailSender).send(any(SimpleMailMessage.class));
        inOrder.verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should create, send and mark notification as SENT for booking cancellation")
    void shouldSendBookingCancelledNotification() {
        List<NotificationStatus> savedStatuses = captureStatusesOnSave();

        notificationService.sendBookingCancelled(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);

        verify(javaMailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@booking.com");
        assertThat(sentMessage.getTo()).containsExactly(CUSTOMER_EMAIL);
        assertThat(sentMessage.getSubject()).isEqualTo("Booking %s cancelled!".formatted(bookingId));
        assertThat(sentMessage.getText())
                .isEqualTo("Hello %s! Your booking %s is cancelled.".formatted(CUSTOMER_NAME, bookingId));

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        Notification sent = notificationCaptor.getAllValues().getLast();
        assertThat(savedStatuses).containsExactly(NotificationStatus.PENDING, NotificationStatus.SENT);
        assertThat(sent.getType()).isEqualTo(NotificationType.BOOKING_CANCELLED);
        assertThat(sent.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(sent.getSentAt()).isEqualTo(FIXED_INSTANT);
        assertThat(sent.getRecipient()).isEqualTo(CUSTOMER_EMAIL);
    }

    @Test
    @DisplayName("Should mark notification as FAILED and not throw when mail sending fails")
    void shouldMarkNotificationAsFailedWhenMailSendingFails() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailSendException("SMTP connection failed"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        notificationService.sendBookingConfirmed(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        Notification failed = notificationCaptor.getAllValues().get(1);
        assertThat(failed.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failed.getSentAt()).isNull();
        assertThat(failed.getType()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
    }

    @Test
    @DisplayName("Should mark cancelled notification as FAILED when mail sending fails")
    void shouldMarkCancelledNotificationAsFailedWhenMailSendingFails() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailSendException("SMTP connection failed"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        notificationService.sendBookingCancelled(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        Notification failed = notificationCaptor.getAllValues().get(1);
        assertThat(failed.getType()).isEqualTo(NotificationType.BOOKING_CANCELLED);
        assertThat(failed.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failed.getSentAt()).isNull();
    }

    @Test
    @DisplayName("Should throw when recipient is blank before sending mail")
    void shouldThrowWhenRecipientIsBlank() {
        assertThatThrownBy(() -> notificationService.sendBookingConfirmed(bookingId, "   ", CUSTOMER_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("notification must have at least one recipient");

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    private List<NotificationStatus> captureStatusesOnSave() {
        List<NotificationStatus> statuses = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            statuses.add(notification.getStatus());
            return notification;
        });
        return statuses;
    }
}
