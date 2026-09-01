package dev.relay.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByDeliveryIdOrderByAttemptNo(UUID deliveryId);

    @Query("""
            select avg(a.latencyMs) from DeliveryAttempt a, Delivery d, Event ev, Endpoint e
            where a.deliveryId = d.id and d.eventId = ev.id and ev.endpointId = e.id
              and e.userId = :userId and a.succeeded = true and a.latencyMs is not null
            """)
    Double averageLatencyForUser(@Param("userId") UUID userId);
}
