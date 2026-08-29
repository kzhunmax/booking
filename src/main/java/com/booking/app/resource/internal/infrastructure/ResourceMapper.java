package com.booking.app.resource.internal.infrastructure;

import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.internal.domain.Resource;

public final class ResourceMapper {

    private ResourceMapper() {}

    public static ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getPublicId(),
                resource.getName(),
                resource.getDescription(),
                resource.getStatus(),
                resource.getPricePerHour(),
                resource.getCurrency());
    }
}
