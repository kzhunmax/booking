package com.booking.app.booking.internal.infrastructure;

import java.math.BigDecimal;

public interface ResourceBookingSummary {

    String getResourceId();

    Long getTotalBookings();

    BigDecimal getTotalRevenue();
}
