package dev.relay.event;

import dev.relay.delivery.Delivery;
import dev.relay.delivery.DeliveryAttempt;
import dev.relay.destination.Destination;

import java.util.List;

/**
 * Read model for the event detail view: an event together with each of its
 * deliveries, the destination it targets, and the full attempt history.
 */
public record EventWithDeliveries(Event event, List<DeliveryDetail> deliveries) {

    public record DeliveryDetail(Delivery delivery, Destination destination, List<DeliveryAttempt> attempts) {
    }
}
