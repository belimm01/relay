package dev.relay.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "relay.delivery")
public class DeliveryProperties {

    /** How long to wait establishing a connection to a destination. */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** How long to wait for a response once connected. */
    private Duration readTimeout = Duration.ofSeconds(10);

    /** Total attempts before a delivery is dead-lettered. */
    private int maxAttempts = 6;

    /** Delay before the first retry; doubles each subsequent attempt. */
    private Duration initialBackoff = Duration.ofSeconds(1);

    /** Upper bound on any single backoff interval. */
    private Duration maxBackoff = Duration.ofMinutes(5);

    /** How many due deliveries a single poll claims. */
    private int batchSize = 50;

    /** How long a claimed delivery is hidden from other pollers while in flight. */
    private Duration lease = Duration.ofSeconds(30);

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getLease() {
        return lease;
    }

    public void setLease(Duration lease) {
        this.lease = lease;
    }
}
