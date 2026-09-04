package com.booking.app.booking.internal.infrastructure;

import com.booking.app.booking.internal.domain.Booking;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByPublicId(UUID publicId);

    Optional<Booking> findByPublicIdAndCustomerEmail(UUID publicId, String customerEmail);
}
