package com.booking.app.resource.internal.infrastructure;

import com.booking.app.resource.internal.domain.Resource;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByPublicId(UUID id);
}
