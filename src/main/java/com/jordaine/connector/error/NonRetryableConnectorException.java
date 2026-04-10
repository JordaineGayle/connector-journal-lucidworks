package com.jordaine.connector.error;

public class NonRetryableConnectorException extends Exception {
    public NonRetryableConnectorException(String message) {
        super(message);
    }

    public NonRetryableConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
