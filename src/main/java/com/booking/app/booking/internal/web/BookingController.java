package com.booking.app.booking.internal.web;

import com.booking.app.booking.AvailableSlotsResponse;
import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.BookingService;
import com.booking.app.booking.BookingStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse created = bookingService.create(
                request.resourceId(),
                request.customerEmail(),
                request.customerName(),
                request.startsAt(),
                request.endsAt());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.publicId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/available-slots")
    public AvailableSlotsResponse getAvailableSlots(
            @RequestParam UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.findAvailableSlots(resourceId, date);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<BookingResponse> getById(@PathVariable UUID publicId) {
        return ResponseEntity.ok(bookingService.findByPublicId(publicId));
    }

    @GetMapping
    public Page<BookingResponse> getBookings(
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return bookingService.findAll(resourceId, status, from, to, pageable);
    }

    @PostMapping("/{publicId}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable UUID publicId) {
        return ResponseEntity.ok(bookingService.cancel(publicId));
    }
}
