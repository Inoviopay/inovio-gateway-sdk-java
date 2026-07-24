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

/** Appendix F — CVV codes. {@code classification} is DERIVED, not from the spec. */
public final class CvvCodes {

    public static final class Info {
        private final String code;
        private final String description;
        private final String classification;

        Info(String code, String description, String classification) {
            this.code = code;
            this.description = description;
            this.classification = classification;
        }

        public String code() { return code; }
        public String description() { return description; }
        /** DERIVED: match | no_match | neutral. */
        public String classification() { return classification; }
    }

    private static final Map<String, Info> BY_CODE;

    static {
        Map<String, Info> m = new HashMap<>();
        m.put("M", new Info("M", "Match", "match"));
        m.put("N", new Info("N", "No Match", "no_match"));
        m.put("P", new Info("P", "Not Processed", "neutral"));
        m.put("S", new Info("S", "Not Supported", "neutral"));
        m.put("U", new Info("U", "Service Not Available", "neutral"));
        m.put("X", new Info("X", "No CVC/CVV/CVV2/CID Response Data Available", "neutral"));
        m.put("", new Info("", "No CVC/CVV/CVV2/CID Response Data Available", "neutral"));
        BY_CODE = Collections.unmodifiableMap(m);
    }

    private CvvCodes() {}

    public static Info get(String code) {
        return code == null ? null : BY_CODE.get(code.trim().toUpperCase());
    }
    public static Map<String, Info> all() { return BY_CODE; }
}
