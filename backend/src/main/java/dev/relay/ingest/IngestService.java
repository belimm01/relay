package dev.relay.ingest;

import dev.relay.delivery.Delivery;
import dev.relay.delivery.DeliveryRepository;
import dev.relay.destination.Destination;
import dev.relay.destination.DestinationRepository;
import dev.relay.endpoint.Endpoint;
import dev.relay.endpoint.EndpointRepository;
import dev.relay.event.Event;
import dev.relay.event.EventCaptured;
import dev.relay.event.EventRepository;
import dev.relay.filter.FilterEvaluator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class IngestService {

    private final EndpointRepository endpoints;
    private final EventRepository events;
    private final DestinationRepository destinations;
    private final DeliveryRepository deliveries;
    private final FilterEvaluator filterEvaluator;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public IngestService(EndpointRepository endpoints,
                         EventRepository events,
                         DestinationRepository destinations,
                         DeliveryRepository deliveries,
                         FilterEvaluator filterEvaluator,
                         ApplicationEventPublisher publisher,
                         Clock clock) {
        this.endpoints = endpoints;
        this.events = events;
        this.destinations = destinations;
        this.deliveries = deliveries;
        this.filterEvaluator = filterEvaluator;
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * Persists an inbound request as an immutable event and queues one delivery per
     * active destination. Returns fast; the actual forwarding happens out of band.
     */
    @Transactional
    public Event capture(String token, IngestCommand command) {
        Endpoint endpoint = endpoints.findByIngestToken(token)
                .filter(Endpoint::isActive)
                .orElseThrow(() -> new EndpointNotFoundException(token));

        Instant now = clock.instant();
        Event event = events.save(Event.builder()
                .endpointId(endpoint.getId())
                .method(command.method())
                .path(command.path())
                .queryString(command.queryString())
                .headers(command.headers())
                .body(command.body())
                .contentType(command.contentType())
                .sourceIp(command.sourceIp())
                .receivedAt(now)
                .build());

        List<Destination> targets = destinations.findByEndpointIdAndActiveTrue(endpoint.getId());
        for (Destination target : targets) {
            if (filterEvaluator.matches(target.getFilterExpr(), event)) {
                deliveries.save(new Delivery(event.getId(), target.getId(), now));
            }
        }

        publisher.publishEvent(new EventCaptured(endpoint.getId(), event.getId()));
        return event;
    }
}
