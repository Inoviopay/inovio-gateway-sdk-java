package com.inoviopay.gateway.errors;

import java.util.Map;

/** Client-side, or API 110-120 — a missing/invalid field; {@code refField} names it. */
public class ValidationException extends InovioException {
    private static final long serialVersionUID = 1L;
    private final Integer code;
    private final String refField;

    public ValidationException(String message) {
        this(message, null, null, null);
    }

    public ValidationException(String message, String refField) {
        this(message, null, refField, null);
    }

    public ValidationException(String message, Integer code, String refField,
                               Map<String, String> raw) {
        super(message, raw);
        this.code = code;
        this.refField = refField;
    }

    public Integer code() { return code; }

    /** The offending wire field, when the gateway named one. */
    public String refField() { return refField; }
}
