package com.booking.app.notification;

import java.util.UUID;

public interface NotificationService {
    void sendBookingConfirmed(UUID bookingId, String recipientEmail, String customerName);

    void sendBookingCancelled(UUID bookingId, String recipientEmail, String customerName);
}
