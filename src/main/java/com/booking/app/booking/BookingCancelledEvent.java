package com.booking.app.booking;

import java.util.UUID;

public record BookingCancelledEvent(UUID bookingId, String customerEmail, String customerName) {}
