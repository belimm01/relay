package dev.relay.destination;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {

    List<Destination> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId);

    List<Destination> findByEndpointIdAndActiveTrue(UUID endpointId);
}
