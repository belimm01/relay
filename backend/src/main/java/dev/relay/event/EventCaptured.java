package dev.relay.event;

import java.util.UUID;

public record EventCaptured(UUID endpointId, UUID eventId) {
}
