package com.booking.app.booking;

import com.booking.app.booking.internal.domain.Booking;
import com.booking.app.booking.internal.domain.BookingInterval;
import com.booking.app.booking.internal.domain.BookingPricing;
import com.booking.app.booking.internal.domain.CustomerDetails;
import com.booking.app.booking.internal.infrastructure.BookingMapper;
import com.booking.app.booking.internal.infrastructure.BookingRepository;
import com.booking.app.booking.internal.infrastructure.BookingSpecifications;
import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final String OVERLAP_BOOKING_CONSTRAINT = "no_overlapping_bookings";
    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final ResourceService resourceService;

    public BookingService(BookingRepository bookingRepository, Clock clock, ResourceService resourceService) {
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.resourceService = resourceService;
    }

    private static Instant later(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    @Transactional
    public BookingResponse create(
            UUID resourceId, String customerEmail, String customerName, Instant startsAt, Instant endsAt) {
        ResourceResponse resourceResponse = resourceService.requireActive(resourceId);
        CustomerDetails details = new CustomerDetails(customerEmail, customerName);
        BookingInterval interval = new BookingInterval(startsAt, endsAt);
        BigDecimal hours = BigDecimal.valueOf(interval.duration().toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = resourceResponse.pricePerHour().multiply(hours).setScale(2, RoundingMode.HALF_UP);
        BookingPricing pricing = new BookingPricing(totalAmount, resourceResponse.currency());
        Booking booking = new Booking(resourceId, details, interval, clock.instant(), pricing);
        persist(booking);
        log.info("Booking created: publicId={}, customerEmail={}", booking.getPublicId(), booking.getCustomerEmail());
        return BookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse findByPublicId(UUID publicId) {
        log.debug("Fetching booking by publicId={}", publicId);
        return BookingMapper.toResponse(requireBooking(publicId));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> findAll(
            UUID resourceId, BookingStatus status, Instant from, Instant to, Pageable pageable) {
        Specification<Booking> spec = BookingSpecifications.filter(resourceId, status, from, to);
        return bookingRepository.findAll(spec, pageable).map(BookingMapper::toResponse);
    }

    @Transactional
    public BookingResponse cancel(UUID publicId) {
        Booking found = requireBooking(publicId);
        found.cancel(clock.instant());
        persist(found);
        log.info("Booking cancelled: publicId={}", publicId);
        return BookingMapper.toResponse(found);
    }

    @Transactional(readOnly = true)
    public AvailableSlotsResponse findAvailableSlots(UUID resourceId, LocalDate date) {
        resourceService.requireActive(resourceId);
        Instant dayStart = date.atStartOfDay(clock.getZone()).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        Specification<Booking> spec = BookingSpecifications.activeBookingsInInterval(resourceId, dayStart, dayEnd);
        List<Booking> bookings = bookingRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "startsAt"));
        List<AvailableSlotsResponse.TimeSlot> slots = availableSlots(bookings, dayStart, dayEnd);
        return new AvailableSlotsResponse(resourceId, date, slots);
    }

    @Transactional
    public BookingResponse confirm(UUID publicId) {
        Booking found = requireBooking(publicId);
        found.confirm();
        persist(found);
        log.info("Booking confirmed: publicId={}", publicId);
        return BookingMapper.toResponse(found);
    }

    private List<AvailableSlotsResponse.TimeSlot> availableSlots(
            List<Booking> bookings, Instant dayStart, Instant dayEnd) {
        Instant now = clock.instant();
        Instant windowStart = later(dayStart, now);
        if (windowStart.isAfter(dayEnd)) {
            return new ArrayList<>();
        }
        Instant cursor = windowStart;
        List<AvailableSlotsResponse.TimeSlot> slots = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getStartsAt().isAfter(cursor)) {
                slots.add(new AvailableSlotsResponse.TimeSlot(cursor, booking.getStartsAt()));
            }
            cursor = later(cursor, booking.getEndsAt());
        }
        if (cursor.isBefore(dayEnd)) {
            slots.add(new AvailableSlotsResponse.TimeSlot(cursor, dayEnd));
        }
        return slots;
    }

    private Booking requireBooking(UUID publicId) {
        return bookingRepository.findByPublicId(publicId).orElseThrow(() -> new BookingNotFoundException(publicId));
    }

    private void persist(Booking booking) {
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            if (isOverlapBooking(e)) {
                throw new BookingSlotAlreadyTakenException(booking.getStartsAt(), booking.getEndsAt(), e);
            }
            throw e;
        }
    }

    private boolean isOverlapBooking(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(OVERLAP_BOOKING_CONSTRAINT);
    }
}
