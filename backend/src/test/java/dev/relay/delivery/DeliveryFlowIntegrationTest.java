package dev.relay.delivery;

import com.sun.net.httpserver.HttpServer;
import dev.relay.endpoint.Endpoint;
import dev.relay.endpoint.EndpointRepository;
import dev.relay.destination.Destination;
import dev.relay.destination.DestinationRepository;
import dev.relay.event.Event;
import dev.relay.event.EventRepository;
import dev.relay.security.TokenGenerator;
import dev.relay.user.User;
import dev.relay.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DeliveryFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("relay.delivery.initial-backoff", () -> "10ms");
        registry.add("relay.delivery.max-backoff", () -> "50ms");
        registry.add("relay.delivery.poll-interval", () -> "3600000");
    }

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository users;
    @Autowired EndpointRepository endpoints;
    @Autowired DestinationRepository destinations;
    @Autowired EventRepository events;
    @Autowired DeliveryRepository deliveries;
    @Autowired DeliveryAttemptRepository attempts;
    @Autowired DeliveryDispatcher dispatcher;
    @Autowired TokenGenerator tokenGenerator;
    @Autowired Clock clock;

    private HttpServer stub;
    private final AtomicInteger received = new AtomicInteger();
    private final AtomicInteger failuresRemaining = new AtomicInteger();

    @BeforeEach
    void startStub() throws IOException {
        stub = HttpServer.create(new InetSocketAddress(0), 0);
        stub.createContext("/hook", exchange -> {
            received.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            int status = failuresRemaining.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0 ? 500 : 200;
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        stub.start();
    }

    @AfterEach
    void stopStub() {
        stub.stop(0);
    }

    private String stubUrl() {
        return "http://localhost:" + stub.getAddress().getPort() + "/hook";
    }

    private Endpoint newEndpointWithDestination() {
        User user = users.save(new User(
                tokenGenerator.newToken() + "@relay.local", tokenGenerator.sha256("k"), clock.instant()));
        Endpoint endpoint = endpoints.save(
                new Endpoint(user.getId(), "orders", tokenGenerator.newToken(), clock.instant()));
        destinations.save(new Destination(endpoint.getId(), "stub", stubUrl(), null, clock.instant()));
        return endpoint;
    }

    private Event onlyEventFor(Endpoint endpoint) {
        List<Event> found = events
                .findByEndpointIdOrderByReceivedAtDesc(endpoint.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent();
        assertThat(found).hasSize(1);
        return found.get(0);
    }

    private ResponseEntity<String> ingest(Endpoint endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity("/in/" + endpoint.getIngestToken(),
                new HttpEntity<>("{\"type\":\"order.created\"}", headers), String.class);
    }

    @Test
    void deliversCapturedEventToDestination() {
        Endpoint endpoint = newEndpointWithDestination();

        ResponseEntity<String> response = ingest(endpoint);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Event event = onlyEventFor(endpoint);
        List<Delivery> queued = deliveries.findByEventIdOrderByCreatedAt(event.getId());
        assertThat(queued).hasSize(1);

        dispatcher.dispatch();
        awaitDeliveryStatus(queued.get(0).getId(), DeliveryStatus.DELIVERED);

        assertThat(received.get()).isEqualTo(1);
        assertThat(attempts.findByDeliveryIdOrderByAttemptNo(queued.get(0).getId())).hasSize(1);
    }

    @Test
    void retriesUntilDestinationSucceeds() {
        failuresRemaining.set(1);
        Endpoint endpoint = newEndpointWithDestination();

        ingest(endpoint);
        Event event = onlyEventFor(endpoint);
        Delivery delivery = deliveries.findByEventIdOrderByCreatedAt(event.getId()).get(0);

        await(Duration.ofSeconds(5), () -> {
            dispatcher.dispatch();
            return statusOf(delivery.getId()) == DeliveryStatus.DELIVERED;
        });

        assertThat(received.get()).isEqualTo(2);
        assertThat(attempts.findByDeliveryIdOrderByAttemptNo(delivery.getId())).hasSize(2);
    }

    private DeliveryStatus statusOf(java.util.UUID deliveryId) {
        return deliveries.findById(deliveryId).map(Delivery::getStatus).orElseThrow();
    }

    private void awaitDeliveryStatus(java.util.UUID deliveryId, DeliveryStatus expected) {
        await(Duration.ofSeconds(5), () -> statusOf(deliveryId) == expected);
    }

    private void await(Duration timeout, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("Condition not met within " + timeout);
    }
}
