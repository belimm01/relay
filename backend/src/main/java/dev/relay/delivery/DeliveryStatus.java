package dev.relay.delivery;

public enum DeliveryStatus {

    PENDING,

    RETRYING,

    DELIVERED,

    FAILED;

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED;
    }
}
