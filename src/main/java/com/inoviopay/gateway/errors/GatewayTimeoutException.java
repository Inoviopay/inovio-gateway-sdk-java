package com.inoviopay.gateway.errors;

/**
 * The gateway did not answer in time. <strong>The transaction state is
 * UNKNOWN</strong> — it may still have been approved.
 *
 * <p>Carries the idempotency key so the caller can resolve the true state with
 * {@code client.status(...)} rather than blindly retrying and double-charging.
 *
 * <p>Named {@code GatewayTimeoutException} rather than {@code TimeoutException}
 * so it cannot be confused with {@code java.util.concurrent.TimeoutException},
 * which partners routinely catch for unrelated reasons.
 */
public class GatewayTimeoutException extends TransportException {
    private static final long serialVersionUID = 1L;

    private final long timeoutMs;
    private final String xtlOrderId;

    public GatewayTimeoutException(String message, long timeoutMs, String xtlOrderId) {
        super(message);
        this.timeoutMs = timeoutMs;
        this.xtlOrderId = xtlOrderId;
    }

    public long timeoutMs() { return timeoutMs; }

    /** The idempotency key, if one was set on the request. */
    public String xtlOrderId() { return xtlOrderId; }

    /** Guidance surfaced on the error itself, since this is the trap case. */
    public String recoveryHint() {
        if (xtlOrderId != null) {
            return "Transaction state is UNKNOWN. Resolve it with client.status("
                + "Refs.xtlOrder(\"" + xtlOrderId + "\")) before retrying — "
                + "a blind retry may double-charge.";
        }
        return "Transaction state is UNKNOWN. No idempotency key was set, so the state "
            + "cannot be resolved by key; set idempotency on future requests.";
    }
}
