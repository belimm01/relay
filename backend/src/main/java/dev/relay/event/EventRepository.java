package dev.relay.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByEndpointIdOrderByReceivedAtDesc(UUID endpointId, Pageable pageable);

    @Query("""
            select count(ev) from Event ev, Endpoint e
            where ev.endpointId = e.id and e.userId = :userId
            """)
    long countByUser(@Param("userId") UUID userId);

    @Query(value = """
            select cast(date_trunc('day', ev.received_at at time zone 'UTC') as date) as day,
                   count(*) as total
            from events ev
            join endpoints e on ev.endpoint_id = e.id
            where e.user_id = :userId and ev.received_at >= :since
            group by day
            order by day
            """, nativeQuery = true)
    List<DailyEventCount> countPerDaySince(@Param("userId") UUID userId, @Param("since") Instant since);
}
