package com.inoviopay.gateway.errors;

import java.util.Map;

/** API tier 100-106 — bad credentials, inactive user, bad site/service. */
public class AuthenticationException extends InovioException {
    private static final long serialVersionUID = 1L;
    private final Integer code;

    public AuthenticationException(String message, Integer code, Map<String, String> raw) {
        super(message, raw);
        this.code = code;
    }

    public Integer code() { return code; }
}
