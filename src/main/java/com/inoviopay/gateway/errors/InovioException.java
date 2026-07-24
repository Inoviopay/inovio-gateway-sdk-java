package com.inoviopay.gateway.errors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base of the SDK exception hierarchy (object model §3.7).
 *
 * <p><strong>A DECLINE IS NEVER THROWN</strong> (Q1). A declined transaction
 * returns normally as {@code TransactionResult} with status {@code DECLINED},
 * carrying the full outcome/AVS/CVV detail. Exceptions mean "your request never
 * got a payment answer", not "the answer was no".
 *
 * <p>Unchecked so partners are not forced into try/catch on every call; the
 * cases that matter (timeout in particular) are documented on the subtypes.
 */
public class InovioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> raw;

    public InovioException(String message) {
        this(message, null, null);
    }

    public InovioException(String message, Map<String, String> raw) {
        this(message, raw, null);
    }

    public InovioException(String message, Map<String, String> raw, Throwable cause) {
        super(message, cause);
        this.raw = raw == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(raw));
    }

    /** Every field the gateway returned, verbatim. */
    public Map<String, String> raw() {
        return raw;
    }
}
