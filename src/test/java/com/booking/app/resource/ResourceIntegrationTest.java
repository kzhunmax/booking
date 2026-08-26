package com.booking.app.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.resource.internal.dto.CreateResourceRequest;
import com.booking.app.resource.internal.dto.UpdateResourceRequest;
import com.booking.app.resource.internal.infrastructure.ResourceRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
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
class ResourceIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    @DisplayName("Concurrent creates with the same name (different case) yield one 201 and one 409")
    void shouldAllowOnlyOneResourceWhenCreatedConcurrentlyWithSameName() throws Exception {
        String name = uniqueName("room");
        String lower = name.toLowerCase(Locale.ROOT);
        String upper = name.toUpperCase(Locale.ROOT);

        List<EntityExchangeResult<Void>> responses =
                runConcurrently(() -> postResource(lower, "first client"), () -> postResource(upper, "second client"));

        assertThat(responses.stream().map(EntityExchangeResult::getStatus).toList())
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(countNonArchivedByNameIgnoreCase(name)).isOne();
    }

    @Test
    @DisplayName("Concurrent updates of two resources to the same name yield one 200 and one 409")
    void shouldAllowOnlyOneResourceWhenUpdatedConcurrentlyToSameName() throws Exception {
        ResourceResponse first = createResource(uniqueName("alpha"), "first");
        ResourceResponse second = createResource(uniqueName("beta"), "second");
        String takenName = uniqueName("shared");

        List<EntityExchangeResult<Void>> responses = runConcurrently(
                () -> putResource(first.publicId(), takenName, "updated by first"),
                () -> putResource(second.publicId(), takenName, "updated by second"));

        assertThat(responses.stream().map(EntityExchangeResult::getStatus).toList())
                .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        assertThat(countNonArchivedByNameIgnoreCase(takenName)).isOne();
    }

    @Test
    @DisplayName("Archived name can be reused and the archived resource is hidden from GET")
    void shouldAllowReusingNameAfterArchive() {
        String name = uniqueName("archive-reuse");
        ResourceResponse created = createResource(name, "to be archived");

        restTestClient
                .delete()
                .uri("/api/resources/{publicId}", created.publicId())
                .exchange()
                .expectStatus()
                .isNoContent();

        restTestClient
                .get()
                .uri("/api/resources/{publicId}", created.publicId())
                .exchange()
                .expectStatus()
                .isNotFound();

        ResourceResponse reused = createResource(name, "recreated after archive");

        assertThat(reused.publicId()).isNotEqualTo(created.publicId());
        assertThat(reused.name()).isEqualTo(name);
        assertThat(countNonArchivedByNameIgnoreCase(name)).isOne();
    }

    private ResourceResponse createResource(String name, String description) {
        ResourceResponse body = restTestClient
                .post()
                .uri("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateResourceRequest(name, description))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ResourceResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    private EntityExchangeResult<Void> postResource(String name, String description) {
        return restTestClient
                .post()
                .uri("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateResourceRequest(name, description))
                .exchange()
                .returnResult(Void.class);
    }

    private EntityExchangeResult<Void> putResource(UUID publicId, String name, String description) {
        return restTestClient
                .put()
                .uri("/api/resources/{publicId}", publicId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateResourceRequest(name, description))
                .exchange()
                .returnResult(Void.class);
    }

    private long countNonArchivedByNameIgnoreCase(String name) {
        return resourceRepository.findAll().stream()
                .filter(resource -> resource.getStatus() != ResourceStatus.ARCHIVED)
                .filter(resource -> resource.getName().equalsIgnoreCase(name))
                .count();
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
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
