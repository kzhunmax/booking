package com.booking.app.booking.internal.web;

import com.booking.app.booking.AvailableSlotsResponse;
import com.booking.app.booking.BookingResponse;
import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.ResourceRevenueResponse;
import com.booking.app.booking.RevenueReportResponse;
import com.booking.app.common.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Bookings",
        description = "Endpoints for scheduling visits, checking slot availability, and managing reservations")
@RequestMapping("/api/bookings")
public interface BookingApi {

    @Operation(
            summary = "Create an appointment reservation",
            description = "Reserves a time slot for the specified resource. Prevents overlapping bookings.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created successfully in PENDING status"),
        @ApiResponse(responseCode = "400", description = "Invalid booking interval or resource unavailable"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required"),
        @ApiResponse(responseCode = "409", description = "Slot collision - Chosen time is already booked")
    })
    @PostMapping
    ResponseEntity<BookingResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody CreateBookingRequest request);

    @Operation(
            summary = "Query available slots",
            description = "Computes and returns free, non-overlapping intervals for a resource on a specific date.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available slots returned successfully"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @GetMapping("/available-slots")
    AvailableSlotsResponse getAvailableSlots(
            @Parameter(description = "ID of the resource to check", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
                    @RequestParam
                    UUID resourceId,
            @Parameter(description = "Target date in ISO format (YYYY-MM-DD)", example = "2026-11-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date);

    @Operation(
            summary = "Get booking by ID",
            description =
                    "Retrieves reservation details. Normal users can only view their own bookings; admins can view any.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking found successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{publicId}")
    ResponseEntity<BookingResponse> getById(
            @Parameter(description = "Public UUID of the booking", example = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
                    @PathVariable
                    UUID publicId,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser);

    @Operation(
            summary = "List bookings",
            description =
                    "Retrieves a paginated list of bookings with optional filters. Regular users see only their own.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings page returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    Page<BookingResponse> getBookings(
            @Parameter(description = "Optional filter by resource ID") @RequestParam(required = false) UUID resourceId,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "Optional filter by booking status") @RequestParam(required = false)
                    BookingStatus status,
            @Parameter(description = "Optional filter start timestamp (ISO-8601 UTC)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Optional filter end timestamp (ISO-8601 UTC)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            Pageable pageable);

    @Operation(
            summary = "Cancel a booking",
            description = "Cancels an existing booking if permitted by cancellation window policies.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cancellation rejected (too late or invalid status)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PostMapping("/{publicId}/cancel")
    ResponseEntity<BookingResponse> cancel(
            @Parameter(description = "Public UUID of the booking") @PathVariable UUID publicId,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser);

    @Operation(
            summary = "Export bookings report to Excel (.xlsx)",
            description = "Generates and downloads an Excel spreadsheet report of bookings. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Excel spreadsheet generated successfully",
                content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ResponseEntity<byte[]> exportToExcel(
            @Parameter(description = "Optional filter start timestamp (ISO-8601 UTC)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Optional filter end timestamp (ISO-8601 UTC)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @Parameter(description = "Optional filter by booking status") @RequestParam(required = false)
                    BookingStatus status);

    @Operation(
            summary = "Revenue report",
            description = "Total revenue and revenue per resource for bookings with the given status, "
                    + "whose start time falls in the range. Uses JPQL queries. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters (e.g. from is after to)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping("/revenue")
    RevenueReportResponse getRevenueReport(
            @Parameter(description = "Range start (ISO-8601 UTC)", example = "2026-11-01T00:00:00Z")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Range end (ISO-8601 UTC)", example = "2026-11-30T23:59:59Z")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @Parameter(description = "Booking status to count (default CONFIRMED)")
                    @RequestParam(defaultValue = "CONFIRMED")
                    BookingStatus status);

    @Operation(
            summary = "Top resources by revenue",
            description = "Resources with the highest revenue for bookings with the given status. "
                    + "Uses a native SQL query. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ranking returned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid limit (must be 1-100)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @GetMapping("/revenue/top")
    List<ResourceRevenueResponse> getTopResources(
            @Parameter(description = "Booking status to count (default CONFIRMED)")
                    @RequestParam(defaultValue = "CONFIRMED")
                    BookingStatus status,
            @Parameter(description = "Number of resources to return (1-100)") @RequestParam(defaultValue = "5")
                    int limit);
}
