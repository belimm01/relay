package dev.relay.delivery;

public record SendResult(
        boolean success,
        Integer statusCode,
        String responseBody,
        int latencyMs,
        String error) {

    public static SendResult of(int statusCode, String body, int latencyMs) {
        boolean ok = statusCode >= 200 && statusCode < 300;
        return new SendResult(ok, statusCode, body, latencyMs, ok ? null : "HTTP " + statusCode);
    }

    public static SendResult transportError(String error, int latencyMs) {
        return new SendResult(false, null, null, latencyMs, error);
    }
}
