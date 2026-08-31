package dev.relay.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByEndpointIdOrderByReceivedAtDesc(UUID endpointId, Pageable pageable);
}
