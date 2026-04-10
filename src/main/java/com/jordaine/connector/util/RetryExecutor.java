package com.jordaine.connector.util;

import com.jordaine.connector.error.RetryableConnectorException;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Executes an operation with retry semantics for {@link RetryableConnectorException}.
 *
 * <p>The executor applies exponential backoff with jitter and honors Retry-After values when the
 * exception provides one.
 */
public class RetryExecutor {

    @FunctionalInterface
    public interface RetryableOperation<T> {
        /**
         * Performs the work that may need to be retried.
         */
        T execute() throws Exception;
    }

    /**
     * Executes the supplied operation until it succeeds or a non-retryable termination condition is
     * reached.
     */
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

    /**
     * Returns {@code true} when the exception type should participate in retry logic.
     */
    private boolean isRetryable(Exception ex) {
        return ex instanceof RetryableConnectorException;
    }

    /**
     * Computes the delay before the next attempt using exponential backoff plus jitter.
     */
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