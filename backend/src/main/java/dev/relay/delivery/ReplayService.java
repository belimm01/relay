package dev.relay.delivery;

import dev.relay.destination.Destination;
import dev.relay.destination.DestinationRepository;
import dev.relay.event.Event;
import dev.relay.event.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReplayService {

    private final EventService events;
    private final DeliveryRepository deliveries;
    private final DestinationRepository destinations;
    private final Clock clock;

    public ReplayService(EventService events,
                         DeliveryRepository deliveries,
                         DestinationRepository destinations,
                         Clock clock) {
        this.events = events;
        this.deliveries = deliveries;
        this.destinations = destinations;
        this.clock = clock;
    }

    @Transactional
    public int replay(UUID userId, UUID eventId) {
        Event event = events.require(userId, eventId);
        Instant now = clock.instant();

        List<Delivery> existing = deliveries.findByEventIdOrderByCreatedAt(eventId);
        if (!existing.isEmpty()) {
            existing.forEach(delivery -> delivery.requeue(now));
            return existing.size();
        }

        List<Destination> targets = destinations.findByEndpointIdAndActiveTrue(event.getEndpointId());
        targets.forEach(target -> deliveries.save(new Delivery(eventId, target.getId(), now)));
        return targets.size();
    }
}
