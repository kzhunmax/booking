package com.booking.app.auth.internal.web;

import com.booking.app.auth.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for updating user account status")
public record UpdateUserStatusRequest(
        @Schema(description = "Target account status (ACTIVE, SUSPENDED)") @NotNull
        UserStatus status) {}
