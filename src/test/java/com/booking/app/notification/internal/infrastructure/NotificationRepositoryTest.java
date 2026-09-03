package com.booking.app.notification.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.config.JpaConfig;
import com.booking.app.notification.NotificationType;
import com.booking.app.notification.internal.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private static Notification notification() {
        return new Notification(
                NotificationType.BOOKING_CONFIRMED,
                "customer@example.com",
                "Booking Confirmed",
                "Your booking is confirmed");
    }

    @Test
    @DisplayName("Should persist notification with generated createdAt")
    void shouldPersistNotificationWithGeneratedFields() {
        Notification notification = notification();
        Notification saved = notificationRepository.saveAndFlush(notification);

        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getSentAt()).isNull();
    }
}
