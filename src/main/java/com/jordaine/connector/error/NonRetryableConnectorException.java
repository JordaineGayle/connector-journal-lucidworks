package com.jordaine.connector.error;

/**
 * Signals a terminal connector failure that should not be retried automatically.
 */
public class NonRetryableConnectorException extends Exception {
    public NonRetryableConnectorException(String message) {
        super(message);
    }

    public NonRetryableConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
