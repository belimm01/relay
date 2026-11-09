package dev.relay.delivery;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    List<Delivery> findByEventIdOrderByCreatedAt(UUID eventId);

    long countByStatus(DeliveryStatus status);

    @Query("""
            select d.status, count(d) from Delivery d, Event ev, Endpoint e
            where d.eventId = ev.id and ev.endpointId = e.id and e.userId = :userId
            group by d.status
            """)
    List<Object[]> countByStatusForUser(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select d from Delivery d
            where d.status in (dev.relay.delivery.DeliveryStatus.PENDING,
                               dev.relay.delivery.DeliveryStatus.RETRYING)
              and d.nextAttemptAt <= :now
            order by d.nextAttemptAt asc
            """)
    List<Delivery> claimDue(@Param("now") Instant now, Pageable pageable);
}
