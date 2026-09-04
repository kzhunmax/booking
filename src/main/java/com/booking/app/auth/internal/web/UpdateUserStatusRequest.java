package com.booking.app.auth.internal.web;

import com.booking.app.auth.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {}
