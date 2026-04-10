package com.jordaine.connector.util;

import com.jordaine.connector.error.RetryableConnectorException;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class RetryExecutor {

    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    public <T> T execute(RetryableOperation<T> operation, int maxRetries, Duration initialBackoff) throws Exception {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }

        if (initialBackoff == null || initialBackoff.isNegative()) {
            throw new IllegalArgumentException("initialBackoff must be >= 0");
        }

        int attempt = 0;

        while (true) {
            try {
                return operation.execute();
            } catch (Exception ex) {
                attempt++;

                if (!isRetryable(ex) || attempt > maxRetries) {
                    throw ex;
                }

                long delayMillis = calculateDelayMillis(initialBackoff, attempt, ex);
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
            }
        }
    }

    private boolean isRetryable(Exception ex) {
        return ex instanceof RetryableConnectorException;
    }

    private long calculateDelayMillis(Duration initialBackoff, int attempt, Exception ex) {
        long base = initialBackoff.toMillis();
        long exponential = base * (1L << Math.min(attempt - 1, 6));
        long jitter = ThreadLocalRandom.current().nextLong(100, 500);
        long backoffDelay = exponential + jitter;

        if (ex instanceof RetryableConnectorException retryableConnectorException) {
            long retryAfterMillis = retryableConnectorException.getRetryAfter()
                    .map(Duration::toMillis)
                    .orElse(0L);
            return Math.max(backoffDelay, retryAfterMillis);
        }

        return backoffDelay;
    }
}