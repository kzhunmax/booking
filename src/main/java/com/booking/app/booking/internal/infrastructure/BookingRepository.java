package com.booking.app.booking.internal.infrastructure;

import com.booking.app.booking.BookingStatus;
import com.booking.app.booking.internal.domain.Booking;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByPublicId(UUID publicId);

    Optional<Booking> findByPublicIdAndCustomerEmail(UUID publicId, String customerEmail);

    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0)
        FROM Booking b
        WHERE b.status = :status
          AND b.startsAt >= :from
          AND b.startsAt <= :to
    """)
    BigDecimal calculateTotalRevenue(
            @Param("status") BookingStatus status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT new com.booking.app.booking.internal.infrastructure.BookingRevenueSummary(
            b.resourcePublicId,
            COUNT(b.id),
            COALESCE(SUM(b.totalAmount), 0)
        )
        FROM Booking b
        WHERE b.status = :status
          AND b.startsAt >= :from
          AND b.startsAt <= :to
        GROUP BY b.resourcePublicId
        ORDER BY COALESCE(SUM(b.totalAmount), 0) DESC
    """)
    List<BookingRevenueSummary> summarizeRevenueByResource(
            @Param("status") BookingStatus status, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
                SELECT CAST(b.resource_id AS VARCHAR) AS resourceId,
                       COUNT(b.id) AS totalBookings,
                       COALESCE(SUM(b.total_amount), 0) AS totalRevenue
                FROM bookings b
                WHERE b.status = :status
                GROUP BY b.resource_id
                ORDER BY totalRevenue DESC
                LIMIT :limit
            """, nativeQuery = true)
    List<ResourceBookingSummary> findTopResourcesByRevenue(@Param("status") String status, @Param("limit") int limit);
}
