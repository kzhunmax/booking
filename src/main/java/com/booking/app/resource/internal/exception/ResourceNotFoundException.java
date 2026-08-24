package com.booking.app.resource.internal.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID id) {
        super("Resource with id %s not found".formatted(id));
    }
}
