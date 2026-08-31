package dev.relay.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An inbound request captured on an endpoint. Events are immutable once written;
 * the durable log is the source of truth for replays.
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    private UUID id;

    @Column(name = "endpoint_id", nullable = false)
    private UUID endpointId;

    @Column(nullable = false)
    private String method;

    @Column
    private String path;

    @Column(name = "query_string")
    private String queryString;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, String> headers;

    @Column
    private String body;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected Event() {
    }

    private Event(Builder builder) {
        this.id = UUID.randomUUID();
        this.endpointId = builder.endpointId;
        this.method = builder.method;
        this.path = builder.path;
        this.queryString = builder.queryString;
        this.headers = builder.headers;
        this.body = builder.body;
        this.contentType = builder.contentType;
        this.sourceIp = builder.sourceIp;
        this.receivedAt = builder.receivedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEndpointId() {
        return endpointId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static final class Builder {
        private UUID endpointId;
        private String method;
        private String path;
        private String queryString;
        private Map<String, String> headers = Map.of();
        private String body;
        private String contentType;
        private String sourceIp;
        private Instant receivedAt;

        public Builder endpointId(UUID endpointId) {
            this.endpointId = endpointId;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder queryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder sourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
            return this;
        }

        public Builder receivedAt(Instant receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public Event build() {
            return new Event(this);
        }
    }
}
