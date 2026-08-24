package com.booking.app.resource.internal.infrastructure;

import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.internal.domain.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getPublicId(),
                resource.getName(),
                resource.getDescription(),
                resource.getStatus().name());
    }
}
