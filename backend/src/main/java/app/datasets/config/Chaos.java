package app.datasets.config;

import java.util.concurrent.ThreadLocalRandom;

/** Small helpers for the deliberately slow and flaky parts of the API. */
public final class Chaos {

    private Chaos() {}

    public static boolean fails(double rate) {
        return rate > 0 && ThreadLocalRandom.current().nextDouble() < rate;
    }

    public static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    public static long between(long min, long max) {
        return max <= min ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
