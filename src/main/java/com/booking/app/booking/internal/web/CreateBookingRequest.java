package com.booking.app.booking.internal.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "resourceId is required") UUID resourceId,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email is invalid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String customerEmail,

        @NotBlank(message = "Name cannot be blank") @Size(max = 255, message = "Name cannot exceed 255 characters")
        String customerName,

        @NotNull(message = "startsAt is required") Instant startsAt,
        @NotNull(message = "endsAt is required") Instant endsAt) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isEndsAtAfterStartsAt() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
