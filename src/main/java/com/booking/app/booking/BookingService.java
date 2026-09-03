package com.booking.app.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingResponse create(
            UUID resourceId, String customerEmail, String customerName, Instant startsAt, Instant endsAt);

    BookingResponse findByPublicId(UUID publicId);

    Page<BookingResponse> findAll(UUID resourceId, BookingStatus status, Instant from, Instant to, Pageable pageable);

    BookingResponse cancel(UUID publicId);

    AvailableSlotsResponse findAvailableSlots(UUID resourceId, LocalDate date);

    BookingResponse confirm(UUID publicId);
}
