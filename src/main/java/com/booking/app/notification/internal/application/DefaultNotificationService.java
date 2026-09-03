package com.booking.app.notification.internal.application;

import com.booking.app.notification.NotificationService;
import com.booking.app.notification.NotificationType;
import com.booking.app.notification.internal.domain.Notification;
import com.booking.app.notification.internal.infrastructure.NotificationRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class DefaultNotificationService implements NotificationService {

    private static final String SENDER_EMAIL = "noreply@booking.com";
    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender javaMailSender;
    private final Clock clock;

    public DefaultNotificationService(
            NotificationRepository notificationRepository, JavaMailSender javaMailSender, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.javaMailSender = javaMailSender;
        this.clock = clock;
    }

    @Override
    public void sendBookingConfirmed(UUID bookingId, String recipientEmail, String customerName) {
        Notification notification = new Notification(
                NotificationType.BOOKING_CONFIRMED,
                recipientEmail,
                "Booking %s confirmed!".formatted(bookingId),
                "Hello %s! Your booking %s is confirmed.".formatted(customerName, bookingId));
        processEmail(notification, recipientEmail);
    }

    @Override
    public void sendBookingCancelled(UUID bookingId, String recipientEmail, String customerName) {
        Notification notification = new Notification(
                NotificationType.BOOKING_CANCELLED,
                recipientEmail,
                "Booking %s cancelled!".formatted(bookingId),
                "Hello %s! Your booking %s is cancelled.".formatted(customerName, bookingId));
        processEmail(notification, recipientEmail);
    }

    private void processEmail(Notification notification, String recipientEmail) {
        notificationRepository.save(notification);
        try {
            createAndSendEmail(recipientEmail, notification.getSubject(), notification.getBody());
            notification.markAsSent(clock.instant());
            notificationRepository.save(notification);
        } catch (MailException ex) {
            notification.markAsFailed();
            notificationRepository.save(notification);
            log.error("Failed to send email to {}", recipientEmail, ex);
        }
    }

    private void createAndSendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SENDER_EMAIL);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }
}
