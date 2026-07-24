package com.inoviopay.gateway.result;

import java.util.Map;

/** Result of TESTAUTH / TESTGW. */
public final class HealthResult {

    private final boolean ok;
    private final String action;
    private final TransactionResult.Outcome outcome;
    private final Map<String, String> raw;

    public HealthResult(boolean ok, String action,
                        TransactionResult.Outcome outcome, Map<String, String> raw) {
        this.ok = ok;
        this.action = action;
        this.outcome = outcome;
        this.raw = raw;
    }

    public boolean ok() { return ok; }
    public String action() { return action; }
    public TransactionResult.Outcome outcome() { return outcome; }
    public Map<String, String> raw() { return raw; }
}
