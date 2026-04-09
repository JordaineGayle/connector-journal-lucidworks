package com.jordaine.connector.util;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class RetryExecutor {

    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    public <T> T execute(RetryableOperation<T> operation, int maxRetries, Duration initialBackoff) throws Exception {
        int attempt = 0;

        while (true) {
            try {
                return operation.execute();
            } catch (Exception ex) {
                attempt++;

                if (!isRetryable(ex) || attempt > maxRetries) {
                    throw ex;
                }

                long delayMillis = calculateDelayMillis(initialBackoff, attempt);
                Thread.sleep(delayMillis);
            }
        }
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof IOException) {
            String message = ex.getMessage();
            if (message == null) {
                return true;
            }

            return message.contains("HTTP 429")
                    || message.contains("HTTP 500")
                    || message.contains("HTTP 502")
                    || message.contains("HTTP 503")
                    || message.contains("HTTP 504")
                    || !message.contains("HTTP ");
        }

        return false;
    }

    private long calculateDelayMillis(Duration initialBackoff, int attempt) {
        long base = initialBackoff.toMillis();
        long exponential = base * (1L << Math.min(attempt - 1, 6));
        long jitter = ThreadLocalRandom.current().nextLong(100, 500);
        return exponential + jitter;
    }
}