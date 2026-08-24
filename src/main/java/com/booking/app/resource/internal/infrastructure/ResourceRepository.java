package com.booking.app.resource.internal.infrastructure;

import com.booking.app.resource.internal.domain.Resource;
import com.booking.app.resource.internal.domain.ResourceStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByPublicId(UUID id);

    Page<Resource> findByStatus(ResourceStatus status, Pageable pageable);
}
