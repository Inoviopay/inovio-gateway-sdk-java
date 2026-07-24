// GENERATED FILE — DO NOT EDIT.
// Source: Inovio Gateway Payments Service API v4.14 (spec/spec-enums.json)
// Regenerate: python3 scripts/generate_enums.py
//
// Classifiers (retryable/terminal/stopRecurring, AVS/CVV classification and the
// API-code -> exception mapping) are DERIVED by the SDK project, not stated in
// the spec. See spec/README.md.
package com.inoviopay.gateway.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Appendix E — AVS codes. {@code classification} is DERIVED, not from the spec. */
public final class AvsCodes {

    public static final class Info {
        private final String code;
        private final String description;
        private final String cardNetwork;
        private final String classification;

        Info(String code, String description, String cardNetwork, String classification) {
            this.code = code;
            this.description = description;
            this.cardNetwork = cardNetwork;
            this.classification = classification;
        }

        public String code() { return code; }
        public String description() { return description; }
        public String cardNetwork() { return cardNetwork; }
        /**
         * DERIVED: positive | partial | negative | neutral. {@code partial} means
         * some elements matched and some did not (e.g. street matches, postal
         * does not). Whether a partial is acceptable is a merchant risk-policy
         * decision — the SDK reports it and does not decide.
         */
        public String classification() { return classification; }
    }

    private static final Map<String, Info> BY_CODE;

    static {
        Map<String, Info> m = new HashMap<>();
        m.put("A", new Info("A", "Street address matches, but 5-digit and 9-digit postal code do not match.", "Standard domestic (US)", "partial"));
        m.put("B", new Info("B", "Street address matches, but postal code not verified.", "Standard international", "neutral"));
        m.put("C", new Info("C", "Street address and postal code do not match.", "Standard international", "negative"));
        m.put("D", new Info("D", "Street address and postal code match. Code \"M\" is equivalent.", "Standard international", "positive"));
        m.put("E", new Info("E", "AVS data is invalid or AVS is not allowed for this card type.", "Standard domestic (US)", "neutral"));
        m.put("F", new Info("F", "Card member's name does not match, but billing postal code matches.", "American Express only", "partial"));
        m.put("G", new Info("G", "Non-U.S. issuing bank does not support AVS.", "Standard international", "neutral"));
        m.put("H", new Info("H", "Card member's name does not match. Street address and postal code match.", "American Express only", "partial"));
        m.put("I", new Info("I", "Address not verified.", "Standard international", "neutral"));
        m.put("J", new Info("J", "Card member's name, billing address, and postal code match.", "American Express only", "positive"));
        m.put("K", new Info("K", "Card member's name matches but billing address and billing postal code do not match.", "American Express only", "partial"));
        m.put("L", new Info("L", "Card member's name and billing postal code match, but billing address does not match.", "American Express only", "partial"));
        m.put("M", new Info("M", "Street address and postal code match. Code \"D\" is equivalent.", "Standard international", "positive"));
        m.put("N", new Info("N", "Street address and postal code do not match.", "Standard domestic (US)", "negative"));
        m.put("O", new Info("O", "Card member's name and billing address match, but billing postal code does not match.", "American Express only", "partial"));
        m.put("P", new Info("P", "Postal code matches, but street address not verified.", "Standard international", "neutral"));
        m.put("Q", new Info("Q", "Card member's name, billing address, and postal code match.", "American Express only", "positive"));
        m.put("R", new Info("R", "System unavailable.", "Standard domestic (US)", "neutral"));
        m.put("S", new Info("S", "Bank does not support AVS.", "Standard domestic (US)", "neutral"));
        m.put("T", new Info("T", "Card member's name does not match, but street address matches.", "American Express only", "partial"));
        m.put("U", new Info("U", "Address information unavailable. Returned if the U.S. bank does not support non-U.S. AVS or if the AVS in a U.S. bank is not functioning properly.", "Standard domestic (US)", "neutral"));
        m.put("V", new Info("V", "Card member's name, billing address, and billing postal code match.", "American Express only", "positive"));
        m.put("W", new Info("W", "Street address does not match, but 9-digit postal code matches.", "Standard domestic (US)", "partial"));
        m.put("X", new Info("X", "Street address and 9-digit postal code match.", "Standard domestic (US)", "positive"));
        m.put("Y", new Info("Y", "Street address and 5-digit postal code match.", "Standard domestic (US)", "positive"));
        m.put("Z", new Info("Z", "Street address does not match, but 5-digit postal code matches.", "Standard domestic (US)", "partial"));
        BY_CODE = Collections.unmodifiableMap(m);
    }

    private AvsCodes() {}

    public static Info get(String code) {
        return code == null ? null : BY_CODE.get(code.trim().toUpperCase());
    }
    public static Map<String, Info> all() { return BY_CODE; }
}
