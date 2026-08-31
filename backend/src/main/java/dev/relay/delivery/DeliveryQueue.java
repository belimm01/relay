package dev.relay.delivery;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DeliveryQueue {

    private final DeliveryRepository deliveries;
    private final DeliveryProperties properties;
    private final Clock clock;

    public DeliveryQueue(DeliveryRepository deliveries, DeliveryProperties properties, Clock clock) {
        this.deliveries = deliveries;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public List<UUID> claimDue() {
        Instant now = clock.instant();
        List<Delivery> due = deliveries.claimDue(now, PageRequest.of(0, properties.getBatchSize()));
        Instant leaseUntil = now.plus(properties.getLease());
        for (Delivery delivery : due) {
            delivery.lease(leaseUntil, now);
        }
        return due.stream().map(Delivery::getId).toList();
    }
}
