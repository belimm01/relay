package dev.relay.ingest;

import dev.relay.event.Event;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @RequestMapping("/in/{token}")
    public ResponseEntity<Map<String, Object>> ingest(@PathVariable String token,
                                                      @RequestBody(required = false) byte[] body,
                                                      HttpServletRequest request) {
        IngestCommand command = new IngestCommand(
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                collectHeaders(request),
                body == null ? null : new String(body, StandardCharsets.UTF_8),
                request.getContentType(),
                clientIp(request));

        Event event = ingestService.capture(token, command);
        return ResponseEntity.accepted().body(Map.of("id", event.getId(), "status", "accepted"));
    }

    private static Map<String, String> collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, String.join(", ", Collections.list(request.getHeaders(name))));
        }
        return headers;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EndpointNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EndpointNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "unknown_endpoint"));
    }
}
