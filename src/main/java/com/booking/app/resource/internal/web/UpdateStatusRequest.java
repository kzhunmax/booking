package com.booking.app.resource.internal.web;

import com.booking.app.resource.ResourceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for updating resource status")
public record UpdateStatusRequest(
        @Schema(description = "New resource status") @NotNull(message = "Status is required")
        ResourceStatus status) {}
