package dev.relay.event;

import dev.relay.common.ResourceNotFoundException;
import dev.relay.delivery.Delivery;
import dev.relay.delivery.DeliveryAttempt;
import dev.relay.delivery.DeliveryAttemptRepository;
import dev.relay.delivery.DeliveryRepository;
import dev.relay.destination.Destination;
import dev.relay.destination.DestinationRepository;
import dev.relay.endpoint.EndpointService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository events;
    private final DeliveryRepository deliveries;
    private final DeliveryAttemptRepository attempts;
    private final DestinationRepository destinations;
    private final EndpointService endpoints;

    public EventService(EventRepository events,
                        DeliveryRepository deliveries,
                        DeliveryAttemptRepository attempts,
                        DestinationRepository destinations,
                        EndpointService endpoints) {
        this.events = events;
        this.deliveries = deliveries;
        this.attempts = attempts;
        this.destinations = destinations;
        this.endpoints = endpoints;
    }

    @Transactional(readOnly = true)
    public Page<Event> list(UUID userId, UUID endpointId, Pageable pageable) {
        endpoints.require(userId, endpointId);
        return events.findByEndpointIdOrderByReceivedAtDesc(endpointId, pageable);
    }

    @Transactional(readOnly = true)
    public Event require(UUID userId, UUID eventId) {
        Event event = events.findById(eventId).orElse(null);
        if (event == null) {
            throw new ResourceNotFoundException("Event", eventId);
        }
        endpoints.require(userId, event.getEndpointId());
        return event;
    }

    @Transactional(readOnly = true)
    public EventWithDeliveries detail(UUID userId, UUID eventId) {
        Event event = require(userId, eventId);
        List<EventWithDeliveries.DeliveryDetail> details = deliveries.findByEventIdOrderByCreatedAt(eventId)
                .stream()
                .map(this::toDetail)
                .toList();
        return new EventWithDeliveries(event, details);
    }

    private EventWithDeliveries.DeliveryDetail toDetail(Delivery delivery) {
        Destination destination = destinations.findById(delivery.getDestinationId()).orElse(null);
        List<DeliveryAttempt> history = attempts.findByDeliveryIdOrderByAttemptNo(delivery.getId());
        return new EventWithDeliveries.DeliveryDetail(delivery, destination, history);
    }
}
