package com.booking.app.resource.internal.web;

import com.booking.app.resource.ResourceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status is required") ResourceStatus status) {}
