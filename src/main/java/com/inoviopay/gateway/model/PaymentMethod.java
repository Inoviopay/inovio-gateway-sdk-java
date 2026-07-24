package com.inoviopay.gateway.model;

/**
 * PaymentMethod — the central polymorphic type (object model §3.2).
 *
 * <p>Absorbs the {@code PMT_NUMB} wire overload: that one field means PAN
 * (card), bank account number (ACH) or IBAN (SEPA/iDEAL/EPS) depending on the
 * rail. The SDK keys the wire semantics off the concrete variant so a partner
 * never sees the overload.
 *
 * <p><strong>Sealed by convention.</strong> On Java 17+ this would be a
 * {@code sealed interface permits Card, Token, ...}. The Java 11 baseline
 * (decision D3) has no {@code sealed}, so the hierarchy is closed the only way
 * 11 allows: every implementation is {@code final} and lives in this package
 * with a package-private constructor. Outside code cannot add a variant.
 */
public interface PaymentMethod {

    /** Discriminator matching the other language SDKs: card, token, savedCard, ... */
    String kind();
}
