package com.booking.app.booking;

import java.util.UUID;

public record BookingConfirmedEvent(UUID bookingId, String customerEmail, String customerName) {}
