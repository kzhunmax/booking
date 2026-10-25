package com.booking.app.booking.internal.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingRevenueSummary(UUID resourceId, Long totalBookings, BigDecimal totalRevenue) {}
