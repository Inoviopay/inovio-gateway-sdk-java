package com.inoviopay.gateway.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** Raw PAN entry — puts the caller in PCI scope. Prefer {@link Token}. */
public final class Card implements PaymentMethod {

    private static final Pattern PAN = Pattern.compile("\\d{12,19}");
    private static final Pattern EXPIRY = Pattern.compile("\\d{6}");
    private static final Pattern CVV = Pattern.compile("\\d{3,4}");

    private final String number;
    private final String expiry;
    private final String cvv;

    Card(String number, String expiry, String cvv) {
        String digits = number == null ? "" : number.replaceAll("[\\s-]", "");
        if (!PAN.matcher(digits).matches()) {
            throw new IllegalArgumentException("card number must be 12-19 digits");
        }
        if (expiry == null || !EXPIRY.matcher(expiry).matches()) {
            throw new IllegalArgumentException(
                "card expiry must be MMYYYY (6 digits), got " + expiry);
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("card expiry month out of range in " + expiry);
        }
        if (cvv != null && !CVV.matcher(cvv).matches()) {
            throw new IllegalArgumentException("card cvv must be 3-4 digits");
        }
        this.number = digits;
        this.expiry = expiry;
        this.cvv = cvv;
    }

    @Override public String kind() { return "card"; }

    /** PAN -> {@code PMT_NUMB} */
    public String number() { return number; }
    /** MMYYYY -> {@code PMT_EXPIRY} */
    public String expiry() { return expiry; }
    /** CVV2/CVC2 -> {@code PMT_KEY} */
    public String cvv() { return cvv; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card c = (Card) o;
        return number.equals(c.number) && expiry.equals(c.expiry) && Objects.equals(cvv, c.cvv);
    }

    @Override public int hashCode() { return Objects.hash(number, expiry, cvv); }

    /** Deliberately does not include the PAN. */
    @Override public String toString() {
        return "Card{last4=" + number.substring(number.length() - 4) + ", expiry=" + expiry + "}";
    }
}
