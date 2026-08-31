package dev.relay.destination;

import dev.relay.common.ResourceNotFoundException;
import dev.relay.endpoint.EndpointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class DestinationService {

    private final DestinationRepository destinations;
    private final EndpointService endpoints;
    private final Clock clock;

    public DestinationService(DestinationRepository destinations, EndpointService endpoints, Clock clock) {
        this.destinations = destinations;
        this.endpoints = endpoints;
        this.clock = clock;
    }

    @Transactional
    public Destination create(UUID userId, UUID endpointId, String name, String targetUrl, String filterExpr) {
        endpoints.require(userId, endpointId);
        Destination destination = new Destination(endpointId, name, targetUrl, filterExpr, clock.instant());
        return destinations.save(destination);
    }

    @Transactional(readOnly = true)
    public List<Destination> list(UUID userId, UUID endpointId) {
        endpoints.require(userId, endpointId);
        return destinations.findByEndpointIdOrderByCreatedAtDesc(endpointId);
    }

    @Transactional
    public Destination update(UUID userId, UUID destinationId, String name, String targetUrl,
                              String filterExpr, Boolean active) {
        Destination destination = require(userId, destinationId);
        if (name != null) {
            destination.setName(name);
        }
        if (targetUrl != null) {
            destination.setTargetUrl(targetUrl);
        }
        if (filterExpr != null) {
            destination.setFilterExpr(filterExpr.isBlank() ? null : filterExpr);
        }
        if (active != null) {
            destination.setActive(active);
        }
        return destination;
    }

    @Transactional
    public void delete(UUID userId, UUID destinationId) {
        Destination destination = require(userId, destinationId);
        destinations.delete(destination);
    }

    private Destination require(UUID userId, UUID destinationId) {
        Destination destination = destinations.findById(destinationId).orElse(null);
        if (destination == null) {
            throw new ResourceNotFoundException("Destination", destinationId);
        }
        endpoints.require(userId, destination.getEndpointId());
        return destination;
    }
}
