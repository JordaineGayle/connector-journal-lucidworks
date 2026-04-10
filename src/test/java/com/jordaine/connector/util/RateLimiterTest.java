package com.jordaine.connector.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests request spacing and argument validation in {@link RateLimiter}.
 */
class RateLimiterTest {

    @Test
    void shouldSpaceRequestsBasedOnConfiguredRate() {
        RateLimiter limiter = new RateLimiter(3000); // 20 ms interval

        long startNanos = System.nanoTime();
        limiter.acquire();
        limiter.acquire();
        limiter.acquire();
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        // First call is immediate; second and third calls should each wait about one interval.
        assertTrue(elapsedMillis >= 35, "Expected at least ~40 ms delay but got " + elapsedMillis + " ms");
    }

    @Test
    void shouldRejectNonPositiveRate() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0));
    }
}
