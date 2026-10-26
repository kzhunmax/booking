package com.booking.app.booking.internal.web;

import com.booking.app.booking.AvailableSlotsResponse;
import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.BookingService;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.ResourceRevenueResponse;
import com.booking.app.booking.RevenueReportResponse;
import com.booking.app.common.security.SecurityUser;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class BookingController implements BookingApi {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public ResponseEntity<BookingResponse> create(SecurityUser currentUser, CreateBookingRequest request) {
        BookingResponse created = bookingService.create(
                request.resourceId(), currentUser.email(), currentUser.name(), request.startsAt(), request.endsAt());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.publicId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    public AvailableSlotsResponse getAvailableSlots(UUID resourceId, LocalDate date) {
        return bookingService.findAvailableSlots(resourceId, date);
    }

    @Override
    public ResponseEntity<BookingResponse> getById(UUID publicId, SecurityUser currentUser) {
        return ResponseEntity.ok(bookingService.findByPublicId(publicId, currentUser.email(), currentUser.isAdmin()));
    }

    @Override
    public Page<BookingResponse> getBookings(
            UUID resourceId,
            SecurityUser currentUser,
            BookingStatus status,
            Instant from,
            Instant to,
            @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable) {
        String customerEmail = currentUser.isAdmin() ? null : currentUser.email();
        return bookingService.findAll(resourceId, customerEmail, status, from, to, pageable);
    }

    @Override
    public ResponseEntity<BookingResponse> cancel(UUID publicId, SecurityUser currentUser) {
        return ResponseEntity.ok(bookingService.cancel(publicId, currentUser.email(), currentUser.isAdmin()));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportToExcel(Instant from, Instant to, BookingStatus status) {
        byte[] excelBytes = bookingService.exportBookingsToExcel(from, to, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookings-report.xlsx\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RevenueReportResponse getRevenueReport(Instant from, Instant to, BookingStatus status) {
        return bookingService.getRevenueReport(from, to, status);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<ResourceRevenueResponse> getTopResources(BookingStatus status, int limit) {
        return bookingService.getTopResourcesByRevenue(status, limit);
    }
}
