package dev.relay.api.dto;

import dev.relay.event.Event;
import dev.relay.event.EventWithDeliveries;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EventDetailResponse(
        UUID id,
        UUID endpointId,
        String method,
        String path,
        String queryString,
        Map<String, String> headers,
        String body,
        String contentType,
        String sourceIp,
        Instant receivedAt,
        List<DeliveryResponse> deliveries) {

    public static EventDetailResponse from(EventWithDeliveries detail) {
        Event event = detail.event();
        return new EventDetailResponse(
                event.getId(),
                event.getEndpointId(),
                event.getMethod(),
                event.getPath(),
                event.getQueryString(),
                event.getHeaders(),
                event.getBody(),
                event.getContentType(),
                event.getSourceIp(),
                event.getReceivedAt(),
                detail.deliveries().stream().map(DeliveryResponse::from).toList());
    }
}
