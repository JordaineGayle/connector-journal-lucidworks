package com.jordaine.connector.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
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
                throw new IOException("HTTP 500");
            }
            return "success";
        }, 5, Duration.ofMillis(10));

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void shouldFailAfterMaxRetries() {
        RetryExecutor executor = new RetryExecutor();

        assertThrows(IOException.class, () ->
                executor.execute(() -> {
                    throw new IOException("HTTP 500");
                }, 2, Duration.ofMillis(10))
        );
    }
}