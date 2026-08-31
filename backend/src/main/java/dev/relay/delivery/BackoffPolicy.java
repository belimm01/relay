package dev.relay.delivery;

import java.time.Duration;
import java.util.function.DoubleSupplier;

/**
 * Exponential backoff with "equal jitter": each interval is half a deterministic
 * exponential step plus a random half. Jitter spreads out retries so a fleet of
 * failed deliveries doesn't stampede a recovering destination.
 */
public class BackoffPolicy {

    private final Duration initial;
    private final Duration max;
    private final DoubleSupplier random;

    public BackoffPolicy(Duration initial, Duration max, DoubleSupplier random) {
        this.initial = initial;
        this.max = max;
        this.random = random;
    }

    /**
     * @param attemptNo the number of the attempt that just failed (1-based)
     * @return how long to wait before the next attempt
     */
    public Duration nextDelay(int attemptNo) {
        int exponent = Math.max(0, attemptNo - 1);
        long cappedMillis = Math.min(
                max.toMillis(),
                initial.toMillis() * (1L << Math.min(exponent, 30)));
        long half = cappedMillis / 2;
        long jittered = half + (long) (random.getAsDouble() * half);
        return Duration.ofMillis(jittered);
    }
}
