package dev.relay.event;

import dev.relay.delivery.Delivery;
import dev.relay.delivery.DeliveryAttempt;
import dev.relay.destination.Destination;

import java.util.List;

public record EventWithDeliveries(Event event, List<DeliveryDetail> deliveries) {

    public record DeliveryDetail(Delivery delivery, Destination destination, List<DeliveryAttempt> attempts) {
    }
}
