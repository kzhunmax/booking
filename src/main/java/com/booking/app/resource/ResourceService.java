package com.booking.app.resource;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceService {

    ResourceResponse findByPublicId(UUID publicId);

    ResourceResponse createResource(String name, String description, BigDecimal pricePerHour, String currency);

    Page<ResourceResponse> findAll(ResourceStatus status, Pageable pageable);

    ResourceResponse update(UUID publicId, String name, String description);

    ResourceResponse updateStatus(UUID publicId, ResourceStatus status);

    void archive(UUID publicId);

    ResourceResponse requireActive(UUID publicId);
}
