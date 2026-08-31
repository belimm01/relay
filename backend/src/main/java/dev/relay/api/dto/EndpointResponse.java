package dev.relay.api.dto;

import dev.relay.endpoint.Endpoint;

import java.time.Instant;
import java.util.UUID;

public record EndpointResponse(
        UUID id,
        String name,
        String ingestToken,
        String ingestPath,
        boolean active,
        Instant createdAt) {

    public static EndpointResponse from(Endpoint endpoint) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getName(),
                endpoint.getIngestToken(),
                "/in/" + endpoint.getIngestToken(),
                endpoint.isActive(),
                endpoint.getCreatedAt());
    }
}
