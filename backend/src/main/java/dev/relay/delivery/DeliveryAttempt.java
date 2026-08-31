package dev.relay.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record of a single HTTP attempt made for a {@link Delivery}.
 */
@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

    @Id
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(nullable = false)
    private boolean succeeded;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected DeliveryAttempt() {
    }

    private DeliveryAttempt(Builder builder) {
        this.id = UUID.randomUUID();
        this.deliveryId = builder.deliveryId;
        this.attemptNo = builder.attemptNo;
        this.succeeded = builder.succeeded;
        this.responseCode = builder.responseCode;
        this.responseBody = builder.responseBody;
        this.errorMessage = builder.errorMessage;
        this.latencyMs = builder.latencyMs;
        this.attemptedAt = builder.attemptedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeliveryId() {
        return deliveryId;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public static final class Builder {
        private UUID deliveryId;
        private int attemptNo;
        private boolean succeeded;
        private Integer responseCode;
        private String responseBody;
        private String errorMessage;
        private Integer latencyMs;
        private Instant attemptedAt;

        public Builder deliveryId(UUID deliveryId) {
            this.deliveryId = deliveryId;
            return this;
        }

        public Builder attemptNo(int attemptNo) {
            this.attemptNo = attemptNo;
            return this;
        }

        public Builder succeeded(boolean succeeded) {
            this.succeeded = succeeded;
            return this;
        }

        public Builder responseCode(Integer responseCode) {
            this.responseCode = responseCode;
            return this;
        }

        public Builder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder latencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public Builder attemptedAt(Instant attemptedAt) {
            this.attemptedAt = attemptedAt;
            return this;
        }

        public DeliveryAttempt build() {
            return new DeliveryAttempt(this);
        }
    }
}
