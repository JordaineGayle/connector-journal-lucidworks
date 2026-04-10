package com.jordaine.connector.error;

import java.time.Duration;
import java.util.Optional;

/**
 * Signals a transient connector failure that may succeed on a later attempt.
 *
 * <p>The optional Retry-After value allows callers to honor server-provided retry guidance when it
 * is available.
 */
public class RetryableConnectorException extends Exception {
    private final Duration retryAfter;

    public RetryableConnectorException(String message) {
        this(message, null, null);
    }

    public RetryableConnectorException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public RetryableConnectorException(String message, Duration retryAfter) {
        this(message, retryAfter, null);
    }

    public RetryableConnectorException(String message, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns an optional server-provided delay that should be respected before retrying.
     */
    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
