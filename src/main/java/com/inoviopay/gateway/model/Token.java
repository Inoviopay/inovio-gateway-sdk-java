package com.inoviopay.gateway.model;

import java.util.Objects;

/**
 * Single-use ephemeral token from the token service -&gt; {@code TOKEN_GUID}.
 *
 * <p>The token replaces ONLY the PAN. Per spec §4.8.2 a token-based transaction
 * still carries {@code PMT_EXPIRY} and {@code PMT_KEY}, so those travel with the
 * token — omitting the expiry yields API 110 "Required field" on
 * {@code REF_FIELD=pmt_expiry}. Verified against the live T1 gateway.
 */
public final class Token implements PaymentMethod {

    private final String guid;
    private final String expiry;
    private final String cvv;

    Token(String guid) {
        this(guid, null, null);
    }

    Token(String guid, String expiry, String cvv) {
        if (guid == null || guid.isEmpty()) {
            throw new IllegalArgumentException("token guid is required");
        }
        if (expiry != null && !expiry.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                "token expiry must be MMYYYY (6 digits), got " + expiry);
        }
        this.guid = guid;
        this.expiry = expiry;
        this.cvv = cvv;
    }

    @Override public String kind() { return "token"; }

    public String guid() { return guid; }

    /** MMYYYY -&gt; {@code PMT_EXPIRY}. Required by the transaction service. */
    public String expiry() { return expiry; }

    /** CVV2/CVC2 -&gt; {@code PMT_KEY}. */
    public String cvv() { return cvv; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token)) return false;
        Token t = (Token) o;
        return guid.equals(t.guid)
            && Objects.equals(expiry, t.expiry)
            && Objects.equals(cvv, t.cvv);
    }

    @Override public int hashCode() { return Objects.hash(guid, expiry, cvv); }

    @Override public String toString() { return "Token{guid=" + guid + "}"; }
}
