package com.booking.app.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.booking.internal.web.CreateBookingRequest;
import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceStatus;
import com.booking.app.resource.internal.web.CreateResourceRequest;
import com.booking.app.resource.internal.web.UpdateStatusRequest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class BookingIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    @DisplayName("Create booking then GET by publicId returns the same PENDING booking")
    void shouldCreateBookingAndFetchByPublicId() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(10);
        BookingResponse created = createBooking(resource.publicId(), slot, slot.plusHours(1));

        BookingResponse fetched = restTestClient
                .get()
                .uri("/api/bookings/{publicId}", created.publicId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(BookingResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.publicId()).isEqualTo(created.publicId());
        assertThat(fetched.resourceId()).isEqualTo(resource.publicId());
        assertThat(fetched.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(fetched.customerEmail()).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("Overlapping booking for the same resource yields 409")
    void shouldRejectOverlappingBookingForSameResource() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(10);
        createBooking(resource.publicId(), slot, slot.plusHours(2));

        restTestClient
                .post()
                .uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingRequest(resource.publicId(), slot.plusHours(1), slot.plusHours(3)))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Booking Slot Taken");
    }

    @Test
    @DisplayName("Cancelled booking frees the slot for a new booking")
    void shouldAllowRebookingAfterCancellation() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(14);
        BookingResponse original = createBooking(resource.publicId(), slot, slot.plusHours(1));

        restTestClient
                .post()
                .uri("/api/bookings/{publicId}/cancel", original.publicId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("CANCELLED");

        BookingResponse replacement = createBooking(resource.publicId(), slot, slot.plusHours(1));

        assertThat(replacement.publicId()).isNotEqualTo(original.publicId());
        assertThat(replacement.status()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("Available slots omit the busy interval of an existing booking")
    void shouldReturnAvailableSlotsExcludingBusyInterval() {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(10);
        createBooking(resource.publicId(), slot, slot.plusHours(1));
        LocalDate date = slot.toLocalDate();

        AvailableSlotsResponse response = restTestClient
                .get()
                .uri("/api/bookings/available-slots?resourceId={resourceId}&date={date}", resource.publicId(), date)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AvailableSlotsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots().getFirst().endsAt()).isEqualTo(slot.toInstant());
        assertThat(response.slots().get(1).startsAt())
                .isEqualTo(slot.plusHours(1).toInstant());
    }

    @Test
    @DisplayName("Booking an inactive resource yields 422")
    void shouldRejectBookingWhenResourceIsInactive() {
        ResourceResponse resource = createResource();
        restTestClient
                .patch()
                .uri("/api/resources/{publicId}/status", resource.publicId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStatusRequest(ResourceStatus.INACTIVE))
                .exchange()
                .expectStatus()
                .isOk();

        ZonedDateTime slot = tomorrowAt(10);
        restTestClient
                .post()
                .uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingRequest(resource.publicId(), slot, slot.plusHours(1)))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Resource Not Available");
    }

    @Test
    @DisplayName("Concurrent overlapping creates yield one 201 and one 409")
    void shouldAllowOnlyOneBookingWhenCreatedConcurrentlyForSameSlot() throws Exception {
        ResourceResponse resource = createResource();
        ZonedDateTime slot = tomorrowAt(16);

        List<EntityExchangeResult<Void>> responses = runConcurrently(
                () -> postBooking(resource.publicId(), slot, slot.plusHours(1)),
                () -> postBooking(resource.publicId(), slot, slot.plusHours(1)));

        assertThat(responses.stream().map(EntityExchangeResult::getStatus).toList())
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
    }

    private ResourceResponse createResource() {
        ResourceResponse body = restTestClient
                .post()
                .uri("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateResourceRequest("room-" + UUID.randomUUID(), "integration room"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ResourceResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    private BookingResponse createBooking(UUID resourceId, ZonedDateTime startsAt, ZonedDateTime endsAt) {
        BookingResponse body = restTestClient
                .post()
                .uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingRequest(resourceId, startsAt, endsAt))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(BookingResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    private EntityExchangeResult<Void> postBooking(UUID resourceId, ZonedDateTime startsAt, ZonedDateTime endsAt) {
        return restTestClient
                .post()
                .uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingRequest(resourceId, startsAt, endsAt))
                .exchange()
                .returnResult(Void.class);
    }

    private static CreateBookingRequest bookingRequest(UUID resourceId, ZonedDateTime startsAt, ZonedDateTime endsAt) {
        return new CreateBookingRequest(
                resourceId, "customer@example.com", "John Doe", startsAt.toInstant(), endsAt.toInstant());
    }

    private static ZonedDateTime tomorrowAt(int hour) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(2).atTime(hour, 0).atZone(ZoneOffset.UTC);
    }

    private static <T> List<T> runConcurrently(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<T> firstResult = executor.submit(gated(ready, start, first));
            Future<T> secondResult = executor.submit(gated(ready, start, second));
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Workers did not become ready");
            }
            start.countDown();
            return List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        }
    }

    private static <T> Callable<T> gated(CountDownLatch ready, CountDownLatch start, Callable<T> delegate) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for start signal");
            }
            return delegate.call();
        };
    }
}
