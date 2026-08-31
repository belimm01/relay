package dev.relay.api.dto;

import dev.relay.delivery.DeliveryAttempt;

import java.time.Instant;

public record AttemptResponse(
        int attemptNo,
        boolean succeeded,
        Integer responseCode,
        String responseBody,
        Integer latencyMs,
        String errorMessage,
        Instant attemptedAt) {

    public static AttemptResponse from(DeliveryAttempt attempt) {
        return new AttemptResponse(
                attempt.getAttemptNo(),
                attempt.isSucceeded(),
                attempt.getResponseCode(),
                attempt.getResponseBody(),
                attempt.getLatencyMs(),
                attempt.getErrorMessage(),
                attempt.getAttemptedAt());
    }
}
