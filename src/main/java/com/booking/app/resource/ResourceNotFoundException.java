package com.booking.app.resource;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID id) {
        super("Resource with id %s not found".formatted(id));
    }
}
