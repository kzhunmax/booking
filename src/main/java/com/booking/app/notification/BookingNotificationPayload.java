package com.booking.app.notification;

import java.time.Instant;
import java.util.UUID;

public record BookingNotificationPayload(
        String eventType, UUID bookingId, String customerEmail, String customerName, Instant timestamp) {}
