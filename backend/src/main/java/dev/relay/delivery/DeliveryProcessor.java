package dev.relay.delivery;

import dev.relay.destination.Destination;
import dev.relay.destination.DestinationRepository;
import dev.relay.event.Event;
import dev.relay.event.EventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class DeliveryProcessor {

    static final String OUTCOME_METRIC = "relay.deliveries";
    static final String OUTCOME_TAG = "outcome";
    static final String OUTCOME_DELIVERED = "delivered";
    static final String OUTCOME_RETRYING = "retrying";
    static final String OUTCOME_DEAD_LETTERED = "dead_lettered";

    private final DeliveryRepository deliveries;
    private final DeliveryAttemptRepository attempts;
    private final DestinationRepository destinations;
    private final EventRepository events;
    private final WebhookSender sender;
    private final BackoffPolicy backoff;
    private final DeliveryProperties properties;
    private final Clock clock;
    private final MeterRegistry meters;

    public DeliveryProcessor(DeliveryRepository deliveries,
                             DeliveryAttemptRepository attempts,
                             DestinationRepository destinations,
                             EventRepository events,
                             WebhookSender sender,
                             BackoffPolicy backoff,
                             DeliveryProperties properties,
                             Clock clock,
                             MeterRegistry meters) {
        this.deliveries = deliveries;
        this.attempts = attempts;
        this.destinations = destinations;
        this.events = events;
        this.sender = sender;
        this.backoff = backoff;
        this.properties = properties;
        this.clock = clock;
        this.meters = meters;
    }

    @Transactional
    public void process(UUID deliveryId) {
        Delivery delivery = deliveries.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus().isTerminal()) {
            return;
        }

        Instant now = clock.instant();
        int attemptNo = delivery.getAttemptCount() + 1;

        Event event = events.findById(delivery.getEventId()).orElse(null);
        if (event == null) {
            delivery.recordFailure(null, "source event missing", null, true, now);
            countOutcome(OUTCOME_DEAD_LETTERED);
            return;
        }

        Destination destination = destinations.findById(delivery.getDestinationId()).orElse(null);
        if (destination == null || !destination.isActive()) {
            recordAttempt(delivery.getId(), attemptNo, SendResult.transportError("destination inactive", 0), now);
            delivery.recordFailure(null, "destination inactive", null, true, now);
            countOutcome(OUTCOME_DEAD_LETTERED);
            return;
        }

        SendResult result = sender.send(destination, event);
        recordAttempt(delivery.getId(), attemptNo, result, now);

        if (result.success()) {
            delivery.recordSuccess(result.statusCode(), now);
            countOutcome(OUTCOME_DELIVERED);
        } else {
            boolean exhausted = attemptNo >= properties.getMaxAttempts();
            Instant retryAt = now.plus(backoff.nextDelay(attemptNo));
            delivery.recordFailure(result.statusCode(), result.error(), retryAt, exhausted, now);
            countOutcome(exhausted ? OUTCOME_DEAD_LETTERED : OUTCOME_RETRYING);
        }
    }

    private void countOutcome(String outcome) {
        meters.counter(OUTCOME_METRIC, OUTCOME_TAG, outcome).increment();
    }

    private void recordAttempt(UUID deliveryId, int attemptNo, SendResult result, Instant now) {
        attempts.save(DeliveryAttempt.builder()
                .deliveryId(deliveryId)
                .attemptNo(attemptNo)
                .succeeded(result.success())
                .responseCode(result.statusCode())
                .responseBody(result.responseBody())
                .errorMessage(result.error())
                .latencyMs(result.latencyMs())
                .attemptedAt(now)
                .build());
    }
}
