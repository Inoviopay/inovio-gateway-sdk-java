package com.inoviopay.gateway.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Money — decimal amount + ISO-4217 currency (object model §3.3 / Q7).
 *
 * <p>The amount is a {@link BigDecimal}, never a {@code double}. Binary floats
 * cannot represent decimal amounts exactly and the wire format is a decimal
 * string like {@code "1.25"}, so the factory rejects floating-point input
 * outright rather than silently corrupting an amount.
 *
 * <p>Java 11 baseline (decision D3): this would be a {@code record} on 17+.
 */
public final class Money {

    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern CURRENCY = Pattern.compile("[A-Za-z]{3}");

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    /**
     * @param amount decimal string, e.g. {@code "1.25"}
     * @param currency ISO-4217 alpha-3, e.g. {@code "USD"}
     */
    public static Money of(String amount, String currency) {
        if (amount == null || !AMOUNT.matcher(amount.trim()).matches()) {
            throw new IllegalArgumentException(
                "Money.of: amount must be a decimal string like \"1.25\", got " + amount);
        }
        return new Money(new BigDecimal(amount.trim()), normalizeCurrency(currency));
    }

    public static Money of(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Money.of: amount is required");
        }
        return new Money(amount, normalizeCurrency(currency));
    }

    /**
     * Rejected on purpose. Binary floating point cannot represent decimal
     * amounts exactly, so accepting a double here would silently corrupt money.
     * Format the value yourself and pass the string.
     */
    public static Money of(double amount, String currency) {
        throw new IllegalArgumentException(
            "Money.of: amount must be a decimal String or BigDecimal, not a double — "
                + "binary floats cannot represent decimal amounts exactly. "
                + "Pass \"1.25\", not 1.25.");
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || !CURRENCY.matcher(currency.trim()).matches()) {
            throw new IllegalArgumentException(
                "Money.of: currency must be an ISO-4217 alpha-3 code like \"USD\", got "
                    + currency);
        }
        return currency.trim().toUpperCase();
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    /** Wire representation of the amount (what goes into {@code LI_VALUE_n}). */
    public String toWire() {
        return amount.toPlainString();
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "cannot combine " + currency + " with " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money other = (Money) o;
        // compareTo, not equals — "1.5" and "1.50" are the same amount
        return currency.equals(other.currency) && amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return toWire() + " " + currency;
    }
}
