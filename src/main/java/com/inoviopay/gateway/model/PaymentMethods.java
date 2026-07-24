package com.inoviopay.gateway.model;

/**
 * Validating constructors for the v1 payment methods.
 *
 * <p>This is the only way to build a {@link PaymentMethod}: the implementation
 * constructors are package-private, which is how the hierarchy stays closed on
 * the Java 11 baseline (no {@code sealed}).
 */
public final class PaymentMethods {

    private PaymentMethods() {}

    public static Card card(String number, String expiry) {
        return new Card(number, expiry, null);
    }

    public static Card card(String number, String expiry, String cvv) {
        return new Card(number, expiry, cvv);
    }

    public static Token token(String guid) {
        return new Token(guid);
    }

    public static SavedCard savedCardByPmtId(String pmtId) {
        return new SavedCard(pmtId, null, null);
    }

    public static SavedCard savedCardByXtlId(String pmtIdXtl) {
        return new SavedCard(null, pmtIdXtl, null);
    }

    public static SavedCard savedCard(String pmtId, String pmtIdXtl, String custId) {
        return new SavedCard(pmtId, pmtIdXtl, custId);
    }

    /** v1 implements card, token and savedCard; other rails fill later phases. */
    public static void assertV1(PaymentMethod pm) {
        if (!(pm instanceof Card || pm instanceof Token || pm instanceof SavedCard)) {
            throw new IllegalArgumentException(
                "payment method \"" + pm.kind() + "\" is declared in the model but not "
                    + "implemented in v1 (v1 supports card, token, savedCard)");
        }
    }
}
