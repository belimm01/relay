package dev.relay.ingest;

import java.util.Map;

/**
 * Everything captured from a raw inbound request, independent of the servlet API.
 */
public record IngestCommand(
        String method,
        String path,
        String queryString,
        Map<String, String> headers,
        String body,
        String contentType,
        String sourceIp) {
}
