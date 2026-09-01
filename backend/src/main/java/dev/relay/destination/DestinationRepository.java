package dev.relay.destination;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {

    List<Destination> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId);

    List<Destination> findByEndpointIdAndActiveTrue(UUID endpointId);

    @Query("""
            select count(d) from Destination d, Endpoint e
            where d.endpointId = e.id and e.userId = :userId
            """)
    long countByUser(@Param("userId") UUID userId);
}
