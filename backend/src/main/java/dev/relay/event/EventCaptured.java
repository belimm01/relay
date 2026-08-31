package dev.relay.event;

import java.util.UUID;

/**
 * Published after an event is persisted so listeners (e.g. the WebSocket
 * broadcaster) can react once the surrounding transaction commits.
 */
public record EventCaptured(UUID endpointId, UUID eventId) {
}
