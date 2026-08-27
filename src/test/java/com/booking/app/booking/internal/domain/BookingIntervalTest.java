package com.booking.app.booking.internal.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BookingIntervalTest {

    private static final Instant BASE = Instant.parse("2026-09-01T10:00:00Z");

    @Nested
    @DisplayName("Creation & Validation")
    class Validation {
        @Test
        @DisplayName("Should create valid interval when endsAt is after startsAt")
        void shouldCreateValidInterval() {
            Instant start = BASE;
            Instant end = BASE.plus(Duration.ofHours(1));
            BookingInterval interval = new BookingInterval(start, end);
            assertThat(interval.startsAt()).isEqualTo(start);
            assertThat(interval.endsAt()).isEqualTo(end);
            assertThat(interval.duration()).isEqualTo(Duration.ofHours(1));
        }

        @Test
        @DisplayName("Should throw exception when startsAt is null")
        void shouldThrowWhenStartsAtIsNull() {
            assertThatThrownBy(() -> new BookingInterval(null, BASE.plus(Duration.ofHours(1))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("startsAt cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when endsAt is null")
        void shouldThrowWhenEndsAtIsNull() {
            assertThatThrownBy(() -> new BookingInterval(BASE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("endsAt cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when endsAt equals startsAt (zero duration)")
        void shouldThrowWhenDurationIsZero() {
            assertThatThrownBy(() -> new BookingInterval(BASE, BASE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ends_at must be after starts_at");
        }

        @Test
        @DisplayName("Should throw exception when endsAt is before startsAt (negative duration)")
        void shouldThrowWhenEndsAtIsBeforeStartsAt() {
            Instant end = BASE.minus(Duration.ofMinutes(30));
            assertThatThrownBy(() -> new BookingInterval(BASE, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ends_at must be after starts_at");
        }
    }

    @Nested
    @DisplayName("Overlap Checks")
    class Overlap {
        @Test
        @DisplayName("Should detect overlapping intervals")
        void shouldDetectOverlap() {
            // [10:00 - 12:00)
            BookingInterval first = new BookingInterval(BASE, BASE.plus(Duration.ofHours(2)));
            // [11:00 - 13:00)
            BookingInterval second =
                    new BookingInterval(BASE.plus(Duration.ofHours(1)), BASE.plus(Duration.ofHours(3)));
            assertThat(first.overlaps(second)).isTrue();
            assertThat(second.overlaps(first)).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when interval null")
        void shouldThrowExceptionWhenIntervalNull() {
            BookingInterval interval = new BookingInterval(BASE, BASE.plus(Duration.ofHours(2)));

            assertThatThrownBy(() -> interval.overlaps(null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should not detect overlap for back-to-back intervals [10:00-11:00) and [11:00-12:00)")
        void shouldNotOverlapAdjacentIntervals() {
            // [10:00 - 11:00)
            BookingInterval first = new BookingInterval(BASE, BASE.plus(Duration.ofHours(1)));
            // [11:00 - 12:00)
            BookingInterval second =
                    new BookingInterval(BASE.plus(Duration.ofHours(1)), BASE.plus(Duration.ofHours(2)));
            assertThat(first.overlaps(second)).isFalse();
            assertThat(second.overlaps(first)).isFalse();
        }
    }
}
