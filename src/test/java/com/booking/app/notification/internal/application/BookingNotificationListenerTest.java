package com.booking.app.notification.internal.application;

import static org.mockito.Mockito.verify;

import com.booking.app.booking.BookingCancelledEvent;
import com.booking.app.booking.BookingConfirmedEvent;
import com.booking.app.notification.NotificationService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String CUSTOMER_NAME = "John Doe";

    @Mock
    private NotificationService notificationService;

    private BookingNotificationListener listener;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        listener = new BookingNotificationListener(notificationService);
        bookingId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should delegate to sendBookingConfirmed when BookingConfirmedEvent received")
    void shouldDelegateOnBookingConfirmed() {
        BookingConfirmedEvent event = new BookingConfirmedEvent(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);

        listener.onBookingConfirmed(event);

        verify(notificationService).sendBookingConfirmed(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);
    }

    @Test
    @DisplayName("Should delegate to sendBookingCancelled when BookingCancelledEvent received")
    void shouldDelegateOnBookingCancelled() {
        BookingCancelledEvent event = new BookingCancelledEvent(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);

        listener.onBookingCancelled(event);

        verify(notificationService).sendBookingCancelled(bookingId, CUSTOMER_EMAIL, CUSTOMER_NAME);
    }
}
