package com.booking.app.booking.internal.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateBookingRequestTest {

    private static final Instant STARTS_AT = Instant.parse("2026-09-01T14:00:00Z");

    @Test
    @DisplayName("isEndsAtAfterStartsAt - returns false when endsAt equals startsAt")
    void shouldReturnFalseForIsEndsAtAfterStartsAtWhenEndsAtEqualsStartsAt() {
        CreateBookingRequest request = new CreateBookingRequest(UUID.randomUUID(), STARTS_AT, STARTS_AT);

        assertThat(request.isEndsAtAfterStartsAt()).isFalse();
    }

    @Test
    @DisplayName("isEndsAtAfterStartsAt - returns true when endsAt is null")
    void shouldReturnFalseForIsEndsAtAfterStartsAtWhenEndsAtIsNull() {
        CreateBookingRequest request = new CreateBookingRequest(UUID.randomUUID(), STARTS_AT, null);

        assertThat(request.endsAt()).isNull();
        assertThat(request.isEndsAtAfterStartsAt()).isTrue();
    }

    @Test
    @DisplayName("isEndsAtAfterStartsAt - returns true when startsAt is null")
    void shouldReturnFalseForIsEndsAtAfterStartsAtWhenStartsAtIsNull() {
        CreateBookingRequest request = new CreateBookingRequest(UUID.randomUUID(), null, STARTS_AT);

        assertThat(request.startsAt()).isNull();
        assertThat(request.isEndsAtAfterStartsAt()).isTrue();
    }
}
