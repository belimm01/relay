package dev.relay.ingest;

import java.util.Map;

public record IngestCommand(
        String method,
        String path,
        String queryString,
        Map<String, String> headers,
        String body,
        String contentType,
        String sourceIp) {
}
