package dev.relay.api.dto;

import dev.relay.event.Event;

import java.time.Instant;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String method,
        String path,
        String contentType,
        String sourceIp,
        Instant receivedAt) {

    public static EventSummary from(Event event) {
        return new EventSummary(
                event.getId(),
                event.getMethod(),
                event.getPath(),
                event.getContentType(),
                event.getSourceIp(),
                event.getReceivedAt());
    }
}
