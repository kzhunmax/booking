package com.booking.app.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    public BookingResponse create(
            UUID resourceId, String customerEmail, String customerName, Instant startsAt, Instant endsAt) {
        throw new UnsupportedOperationException("BookingService is not implemented yet");
    }

    public BookingResponse findByPublicId(UUID publicId) {
        throw new UnsupportedOperationException("BookingService is not implemented yet");
    }

    public Page<BookingResponse> findAll(
            UUID resourceId, BookingStatus status, Instant from, Instant to, Pageable pageable) {
        throw new UnsupportedOperationException("BookingService is not implemented yet");
    }

    public BookingResponse cancel(UUID publicId) {
        throw new UnsupportedOperationException("BookingService is not implemented yet");
    }

    public AvailableSlotsResponse findAvailableSlots(UUID resourceId, LocalDate date) {
        throw new UnsupportedOperationException("BookingService is not implemented yet");
    }
}
