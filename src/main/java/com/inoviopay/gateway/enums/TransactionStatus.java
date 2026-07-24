// GENERATED FILE — DO NOT EDIT.
// Source: Inovio Gateway Payments Service API v4.14 (api-sdk/spec/spec-enums.json)
// Regenerate: python3 scripts/generate_enums.py
//
// Classifiers (retryable/terminal/stopRecurring, AVS/CVV classification and the
// API-code -> exception mapping) are DERIVED by the SDK project, not stated in
// the spec. See api-sdk/spec/README.md.
package com.inoviopay.gateway.enums;

/** Appendix B — the master transaction lifecycle (5 states). */
public enum TransactionStatus {
    /** Transaction has been approved. */
    APPROVED,
    /** Transaction has been declined. */
    DECLINED,
    /** Transaction is in pending status (expected on 3-D Secure, and preauthorization of online check transactions (i.e. Boleto, ACH, Pix etc.)). */
    PENDING,
    /** Transaction processing is not completed or is waiting completion. */
    RUNNING,
    /** Transaction did not finish payment completion (used in European Direct Debit transactions) */
    FAILED;

    /** Parse a wire value; unknown input must never read as APPROVED. */
    public static TransactionStatus fromWire(String raw) {
        if (raw == null) return FAILED;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FAILED;
        }
    }

    /** PENDING or RUNNING — a genuine grouping, not an alias for the status. */
    public boolean isSettling() {
        return this == PENDING || this == RUNNING;
    }
}
