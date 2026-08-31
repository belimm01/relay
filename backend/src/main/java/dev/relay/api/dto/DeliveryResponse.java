package dev.relay.api.dto;

import dev.relay.delivery.DeliveryStatus;
import dev.relay.destination.Destination;
import dev.relay.event.EventWithDeliveries.DeliveryDetail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID destinationId,
        String destinationName,
        String targetUrl,
        DeliveryStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        Integer lastResponseCode,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        List<AttemptResponse> attempts) {

    public static DeliveryResponse from(DeliveryDetail detail) {
        Destination destination = detail.destination();
        return new DeliveryResponse(
                detail.delivery().getId(),
                detail.delivery().getDestinationId(),
                destination == null ? null : destination.getName(),
                destination == null ? null : destination.getTargetUrl(),
                detail.delivery().getStatus(),
                detail.delivery().getAttemptCount(),
                detail.delivery().getNextAttemptAt(),
                detail.delivery().getLastResponseCode(),
                detail.delivery().getLastError(),
                detail.delivery().getCreatedAt(),
                detail.delivery().getUpdatedAt(),
                detail.attempts().stream().map(AttemptResponse::from).toList());
    }
}
