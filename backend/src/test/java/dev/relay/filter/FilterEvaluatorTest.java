package dev.relay.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.relay.event.Event;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterEvaluatorTest {

    private final FilterEvaluator evaluator = new FilterEvaluator(new ObjectMapper());

    private Event event(String body, Map<String, String> headers) {
        return Event.builder()
                .method("POST")
                .headers(headers)
                .body(body)
                .contentType("application/json")
                .receivedAt(Instant.EPOCH)
                .build();
    }

    @Test
    void blankFilterMatchesEverything() {
        assertThat(evaluator.matches(null, event("{}", Map.of()))).isTrue();
        assertThat(evaluator.matches("  ", event("{}", Map.of()))).isTrue();
    }

    @Test
    void matchesStringEquality() {
        Event event = event("{\"type\":\"payment.succeeded\"}", Map.of());
        assertThat(evaluator.matches("body.type == \"payment.succeeded\"", event)).isTrue();
        assertThat(evaluator.matches("body.type == \"payment.failed\"", event)).isFalse();
    }

    @Test
    void matchesNumericComparisons() {
        Event event = event("{\"amount\":150}", Map.of());
        assertThat(evaluator.matches("body.amount >= 100", event)).isTrue();
        assertThat(evaluator.matches("body.amount > 150", event)).isFalse();
        assertThat(evaluator.matches("body.amount <= 150", event)).isTrue();
    }

    @Test
    void supportsHeadersAndConjunction() {
        Event event = event("{\"type\":\"order\",\"total\":42}", Map.of("X-Env", "prod"));
        assertThat(evaluator.matches("body.type == \"order\" && headers.X-Env == \"prod\"", event)).isTrue();
        assertThat(evaluator.matches("body.type == \"order\" && body.total > 100", event)).isFalse();
    }

    @Test
    void supportsContains() {
        Event event = event("{\"type\":\"payment.succeeded\"}", Map.of());
        assertThat(evaluator.matches("body.type contains \"payment\"", event)).isTrue();
        assertThat(evaluator.matches("body.type contains \"refund\"", event)).isFalse();
    }

    @Test
    void missingFieldDoesNotMatchEquality() {
        Event event = event("{\"type\":\"order\"}", Map.of());
        assertThat(evaluator.matches("body.customer == \"acme\"", event)).isFalse();
        assertThat(evaluator.matches("body.customer != \"acme\"", event)).isTrue();
    }

    @Test
    void nonJsonBodyDoesNotThrow() {
        Event event = event("not-json", Map.of());
        assertThat(evaluator.matches("body.type == \"order\"", event)).isFalse();
    }
}
