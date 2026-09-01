package dev.relay.endpoint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {

    Optional<Endpoint> findByIngestToken(String ingestToken);

    List<Endpoint> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndActiveTrue(UUID userId);
}
