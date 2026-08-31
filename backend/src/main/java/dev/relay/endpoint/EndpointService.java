package dev.relay.endpoint;

import dev.relay.common.ResourceNotFoundException;
import dev.relay.security.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class EndpointService {

    private final EndpointRepository endpoints;
    private final TokenGenerator tokenGenerator;
    private final Clock clock;

    public EndpointService(EndpointRepository endpoints, TokenGenerator tokenGenerator, Clock clock) {
        this.endpoints = endpoints;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
    }

    @Transactional
    public Endpoint create(UUID userId, String name) {
        Endpoint endpoint = new Endpoint(userId, name, tokenGenerator.newToken(), clock.instant());
        return endpoints.save(endpoint);
    }

    @Transactional(readOnly = true)
    public List<Endpoint> list(UUID userId) {
        return endpoints.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Endpoint require(UUID userId, UUID endpointId) {
        Endpoint endpoint = endpoints.findById(endpointId).orElse(null);
        if (endpoint == null || !endpoint.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Endpoint", endpointId);
        }
        return endpoint;
    }

    @Transactional
    public Endpoint setActive(UUID userId, UUID endpointId, boolean active) {
        Endpoint endpoint = require(userId, endpointId);
        endpoint.setActive(active);
        return endpoint;
    }

    @Transactional
    public void delete(UUID userId, UUID endpointId) {
        Endpoint endpoint = require(userId, endpointId);
        endpoints.delete(endpoint);
    }
}
