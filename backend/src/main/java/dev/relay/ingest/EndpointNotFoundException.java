package dev.relay.ingest;

public class EndpointNotFoundException extends RuntimeException {

    public EndpointNotFoundException(String token) {
        super("No active endpoint for token: " + token);
    }
}
