package dev.relay.delivery;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.DoubleSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffPolicyTest {

    private static final DoubleSupplier NO_JITTER = () -> 0.0;
    private static final DoubleSupplier FULL_JITTER = () -> 1.0;

    @Test
    void appliesExponentialGrowthWithoutJitter() {
        BackoffPolicy policy = new BackoffPolicy(Duration.ofSeconds(1), Duration.ofMinutes(5), NO_JITTER);

        // With zero jitter the delay is exactly half the exponential step.
        assertThat(policy.nextDelay(1)).isEqualTo(Duration.ofMillis(500));
        assertThat(policy.nextDelay(2)).isEqualTo(Duration.ofMillis(1000));
        assertThat(policy.nextDelay(3)).isEqualTo(Duration.ofMillis(2000));
    }

    @Test
    void fullJitterReachesTheExponentialStep() {
        BackoffPolicy policy = new BackoffPolicy(Duration.ofSeconds(1), Duration.ofMinutes(5), FULL_JITTER);

        assertThat(policy.nextDelay(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.nextDelay(3)).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void neverExceedsTheConfiguredMaximum() {
        BackoffPolicy policy = new BackoffPolicy(Duration.ofSeconds(1), Duration.ofMinutes(5), FULL_JITTER);

        assertThat(policy.nextDelay(30)).isEqualTo(Duration.ofMinutes(5));
    }
}
