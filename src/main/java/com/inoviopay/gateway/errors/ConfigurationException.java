package com.inoviopay.gateway.errors;

import java.util.Map;

/** Currency/product/merchant-account not configured (155, 165, 210, 500...). */
public class ConfigurationException extends InovioException {
    private static final long serialVersionUID = 1L;
    private final Integer code;

    public ConfigurationException(String message, Integer code, Map<String, String> raw) {
        super(message, raw);
        this.code = code;
    }

    public Integer code() { return code; }
}
