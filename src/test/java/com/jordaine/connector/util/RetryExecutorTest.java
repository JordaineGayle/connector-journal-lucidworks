package com.jordaine.connector.util;

import com.jordaine.connector.error.NonRetryableConnectorException;
import com.jordaine.connector.error.RetryableConnectorException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {

    @Test
    void shouldRetryAndSucceed() throws Exception {
        RetryExecutor executor = new RetryExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RetryableConnectorException("HTTP 500");
            }
            return "success";
        }, 5, Duration.ofMillis(10));

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void shouldFailAfterMaxRetries() {
        RetryExecutor executor = new RetryExecutor();

        assertThrows(NonRetryableConnectorException.class, () ->
                executor.execute(() -> {
                    throw new NonRetryableConnectorException("HTTP 400");
                }, 2, Duration.ofMillis(10))
        );
    }

    @Test
    void shouldStopAfterConfiguredMaxRetriesForRetryableError() {
        RetryExecutor executor = new RetryExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(RetryableConnectorException.class, () ->
                executor.execute(() -> {
                    attempts.incrementAndGet();
                    throw new RetryableConnectorException("still failing");
                }, 2, Duration.ofMillis(10))
        );

        // 1 initial attempt + 2 retries
        assertEquals(3, attempts.get());
    }

    @Test
    void shouldRejectNegativeMaxRetries() {
        RetryExecutor executor = new RetryExecutor();

        assertThrows(IllegalArgumentException.class, () ->
                executor.execute(() -> "ok", -1, Duration.ofMillis(10))
        );
    }

    @Test
    void shouldRejectNullOrNegativeBackoff() {
        RetryExecutor executor = new RetryExecutor();

        assertThrows(IllegalArgumentException.class, () ->
                executor.execute(() -> "ok", 1, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                executor.execute(() -> "ok", 1, Duration.ofMillis(-1))
        );
    }

    @Test
    void shouldHonorRetryAfterWhenLargerThanBackoff() throws Exception {
        RetryExecutor executor = new RetryExecutor();
        AtomicInteger attempts = new AtomicInteger(0);
        Duration retryAfter = Duration.ofMillis(350);

        long startNanos = System.nanoTime();

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new RetryableConnectorException("429", retryAfter);
            }
            return "ok";
        }, 1, Duration.ofMillis(10));

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        assertEquals("ok", result);
        assertTrue(elapsedMillis >= 300, "Expected retry delay to honor Retry-After, elapsed=" + elapsedMillis + " ms");
    }
}