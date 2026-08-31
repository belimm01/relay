package dev.relay.delivery;

import dev.relay.destination.Destination;
import dev.relay.destination.DestinationRepository;
import dev.relay.event.Event;
import dev.relay.event.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryProcessorTest {

    private final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    private final DeliveryAttemptRepository attempts = mock(DeliveryAttemptRepository.class);
    private final DestinationRepository destinations = mock(DestinationRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private final WebhookSender sender = mock(WebhookSender.class);

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final BackoffPolicy backoff = new BackoffPolicy(Duration.ofSeconds(1), Duration.ofMinutes(5), () -> 0.0);
    private final DeliveryProperties properties = new DeliveryProperties();

    private DeliveryProcessor processor;
    private Event event;
    private Destination destination;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        processor = new DeliveryProcessor(deliveries, attempts, destinations, events, sender, backoff, properties, clock);

        event = Event.builder()
                .method("POST")
                .headers(Map.of())
                .body("{}")
                .receivedAt(clock.instant())
                .build();
        destination = new Destination(event.getEndpointId(), "target", "https://example.test/hook", null, clock.instant());
        delivery = new Delivery(event.getId(), destination.getId(), clock.instant());

        when(deliveries.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(events.findById(delivery.getEventId())).thenReturn(Optional.of(event));
        when(destinations.findById(delivery.getDestinationId())).thenReturn(Optional.of(destination));
    }

    @Test
    void marksDeliveredOnSuccess() {
        when(sender.send(destination, event)).thenReturn(SendResult.of(200, "ok", 12));

        processor.process(delivery.getId());

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getNextAttemptAt()).isNull();

        ArgumentCaptor<DeliveryAttempt> captor = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(attempts).save(captor.capture());
        assertThat(captor.getValue().isSucceeded()).isTrue();
        assertThat(captor.getValue().getAttemptNo()).isEqualTo(1);
    }

    @Test
    void schedulesRetryOnFailure() {
        when(sender.send(destination, event)).thenReturn(SendResult.of(503, "unavailable", 8));

        processor.process(delivery.getId());

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        // First failure: half of the 1s initial step with zero jitter.
        assertThat(delivery.getNextAttemptAt()).isEqualTo(clock.instant().plusMillis(500));
    }

    @Test
    void deadLettersWhenAttemptsExhausted() {
        properties.setMaxAttempts(1);
        when(sender.send(destination, event)).thenReturn(SendResult.of(500, "boom", 8));

        processor.process(delivery.getId());

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getNextAttemptAt()).isNull();
    }

    @Test
    void skipsInactiveDestinationWithoutSending() {
        destination.setActive(false);

        processor.process(delivery.getId());

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        verify(sender, never()).send(any(), any());
    }
}
