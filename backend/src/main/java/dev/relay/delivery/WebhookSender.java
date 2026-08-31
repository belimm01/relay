package dev.relay.delivery;

import dev.relay.destination.Destination;
import dev.relay.event.Event;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Forwards a captured event to a destination, preserving the original method and
 * body. Non-2xx responses are returned as results (not thrown) so the dispatcher
 * can decide whether to retry.
 */
@Component
public class WebhookSender {

    private static final int MAX_STORED_BODY = 8192;

    private final RestClient restClient;
    private final Clock clock;

    public WebhookSender(RestClient deliveryRestClient, Clock clock) {
        this.restClient = deliveryRestClient;
        this.clock = clock;
    }

    public SendResult send(Destination destination, Event event) {
        long start = clock.millis();
        try {
            byte[] body = event.getBody() == null
                    ? new byte[0]
                    : event.getBody().getBytes(StandardCharsets.UTF_8);

            ResponseEntity<String> response = restClient
                    .method(HttpMethod.valueOf(event.getMethod()))
                    .uri(destination.getTargetUrl())
                    .headers(headers -> {
                        if (event.getContentType() != null) {
                            headers.set("Content-Type", event.getContentType());
                        }
                        headers.set("X-Relay-Event-Id", event.getId().toString());
                    })
                    .body(body)
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> { })
                    .toEntity(String.class);

            int latency = (int) (clock.millis() - start);
            return SendResult.of(response.getStatusCode().value(), truncate(response.getBody()), latency);
        } catch (Exception ex) {
            int latency = (int) (clock.millis() - start);
            return SendResult.transportError(ex.getMessage(), latency);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= MAX_STORED_BODY ? body : body.substring(0, MAX_STORED_BODY);
    }
}
