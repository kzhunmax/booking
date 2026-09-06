package com.booking.app.notification.internal.application;

import com.booking.app.booking.BookingCancelledEvent;
import com.booking.app.booking.BookingConfirmedEvent;
import com.booking.app.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final NotificationService notificationService;

    public BookingNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Received BookingConfirmedEvent for bookingId={}", event.bookingId());
        notificationService.sendBookingConfirmed(event.bookingId(), event.customerEmail(), event.customerName());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Received BookingCancelledEvent for bookingId={}", event.bookingId());
        notificationService.sendBookingCancelled(event.bookingId(), event.customerEmail(), event.customerName());
    }
}
