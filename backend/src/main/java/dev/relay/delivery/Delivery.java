package dev.relay.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks the delivery of one event to one destination across its retry lifecycle.
 * Individual HTTP attempts are recorded as {@link DeliveryAttempt} rows.
 */
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "destination_id", nullable = false)
    private UUID destinationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_response_code")
    private Integer lastResponseCode;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Delivery() {
    }

    public Delivery(UUID eventId, UUID destinationId, Instant now) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.destinationId = destinationId;
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void recordSuccess(int responseCode, Instant now) {
        this.attemptCount++;
        this.status = DeliveryStatus.DELIVERED;
        this.lastResponseCode = responseCode;
        this.lastError = null;
        this.nextAttemptAt = null;
        this.updatedAt = now;
    }

    public void recordFailure(Integer responseCode, String error, Instant retryAt, boolean exhausted, Instant now) {
        this.attemptCount++;
        this.lastResponseCode = responseCode;
        this.lastError = truncate(error);
        this.updatedAt = now;
        if (exhausted) {
            this.status = DeliveryStatus.FAILED;
            this.nextAttemptAt = null;
        } else {
            this.status = DeliveryStatus.RETRYING;
            this.nextAttemptAt = retryAt;
        }
    }

    /**
     * Resets this delivery for a manual replay: fresh retry budget, due immediately.
     */
    public void requeue(Instant now) {
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.lastError = null;
        this.lastResponseCode = null;
        this.nextAttemptAt = now;
        this.updatedAt = now;
    }

    /**
     * Hides this delivery from other pollers until {@code leaseUntil} while it is
     * being processed. If the worker dies, the lease expires and it is retried.
     */
    public void lease(Instant leaseUntil, Instant now) {
        this.nextAttemptAt = leaseUntil;
        this.updatedAt = now;
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 1024 ? error : error.substring(0, 1024);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Integer getLastResponseCode() {
        return lastResponseCode;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Delivery other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
