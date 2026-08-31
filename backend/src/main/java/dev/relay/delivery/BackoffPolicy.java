package dev.relay.delivery;

import java.time.Duration;
import java.util.function.DoubleSupplier;

public class BackoffPolicy {

    private final Duration initial;
    private final Duration max;
    private final DoubleSupplier random;

    public BackoffPolicy(Duration initial, Duration max, DoubleSupplier random) {
        this.initial = initial;
        this.max = max;
        this.random = random;
    }

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
