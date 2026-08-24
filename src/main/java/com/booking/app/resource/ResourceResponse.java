package com.booking.app.resource;

import java.util.UUID;

public record ResourceResponse(UUID publicId, String name, String description, ResourceStatus status) {}
