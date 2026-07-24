package com.inoviopay.gateway.errors;

import java.util.Map;

/** API 100 — throttled. */
public class RateLimitException extends InovioException {
    private static final long serialVersionUID = 1L;

    public RateLimitException(String message, Map<String, String> raw) {
        super(message, raw);
    }
}
