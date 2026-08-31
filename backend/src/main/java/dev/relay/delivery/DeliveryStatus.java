package dev.relay.delivery;

public enum DeliveryStatus {

    /** Queued, not yet attempted. */
    PENDING,

    /** Last attempt failed; scheduled for another try. */
    RETRYING,

    /** Delivered successfully. Terminal. */
    DELIVERED,

    /** Retries exhausted. Terminal (dead-lettered). */
    FAILED;

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED;
    }
}
