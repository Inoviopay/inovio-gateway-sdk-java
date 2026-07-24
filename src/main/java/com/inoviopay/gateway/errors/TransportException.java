package com.inoviopay.gateway.errors;

/** Network-level failure — the request may or may not have been processed. */
public class TransportException extends InovioException {
    private static final long serialVersionUID = 1L;

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, null, cause);
    }
}
