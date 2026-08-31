package dev.relay.api.dto;

import dev.relay.destination.Destination;

import java.time.Instant;
import java.util.UUID;

public record DestinationResponse(
        UUID id,
        UUID endpointId,
        String name,
        String targetUrl,
        String filterExpr,
        boolean active,
        Instant createdAt) {

    public static DestinationResponse from(Destination destination) {
        return new DestinationResponse(
                destination.getId(),
                destination.getEndpointId(),
                destination.getName(),
                destination.getTargetUrl(),
                destination.getFilterExpr(),
                destination.isActive(),
                destination.getCreatedAt());
    }
}
