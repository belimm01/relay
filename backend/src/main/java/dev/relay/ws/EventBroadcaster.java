package dev.relay.ws;

import dev.relay.api.dto.EventSummary;
import dev.relay.event.Event;
import dev.relay.event.EventCaptured;
import dev.relay.event.EventRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes newly captured events to subscribers of
 * {@code /topic/endpoints/{endpointId}/events}. Runs after commit so the
 * dashboard never receives an event it cannot yet read back.
 */
@Component
public class EventBroadcaster {

    private final SimpMessagingTemplate messaging;
    private final EventRepository events;

    public EventBroadcaster(SimpMessagingTemplate messaging, EventRepository events) {
        this.messaging = messaging;
        this.events = events;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCaptured(EventCaptured captured) {
        events.findById(captured.eventId()).ifPresent(event -> {
            String destination = "/topic/endpoints/" + captured.endpointId() + "/events";
            messaging.convertAndSend(destination, EventSummary.from(event));
        });
    }
}
