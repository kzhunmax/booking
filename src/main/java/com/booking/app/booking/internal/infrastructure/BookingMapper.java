package com.booking.app.booking.internal.infrastructure;

import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.internal.domain.Booking;

public final class BookingMapper {
    private BookingMapper() {}

    public static BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getPublicId(),
                booking.getResourcePublicId(),
                booking.getCustomerEmail(),
                booking.getCustomerName(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCurrency());
    }
}
