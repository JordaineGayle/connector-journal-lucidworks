package com.jordaine.connector.error;

import java.time.Duration;
import java.util.Optional;

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

    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
