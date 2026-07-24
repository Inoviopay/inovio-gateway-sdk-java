#!/usr/bin/env python3
"""Generate Java enum/lookup sources from this repo's spec/spec-enums.json (decision D1)."""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = HERE.parent / "spec" / "spec-enums.json"
PKG = HERE.parent / "src/main/java/com/inoviopay/gateway/enums"
spec = json.loads(SPEC.read_text())
A, ver = spec["appendices"], spec["apiVersion"]
PKG.mkdir(parents=True, exist_ok=True)

HEADER = f"""// GENERATED FILE — DO NOT EDIT.
// Source: Inovio Gateway Payments Service API v{ver} (spec/spec-enums.json)
// Regenerate: python3 scripts/generate_enums.py
//
// Classifiers (retryable/terminal/stopRecurring, AVS/CVV classification and the
// API-code -> exception mapping) are DERIVED by the SDK project, not stated in
// the spec. See spec/README.md.
package com.inoviopay.gateway.enums;
"""

def j(s):
    return json.dumps(s)

# --- TransactionStatus ------------------------------------------------------
b = A["B_transactionStatus"]
(PKG / "TransactionStatus.java").write_text(HEADER + f"""
/** Appendix B — the master transaction lifecycle (5 states). */
public enum TransactionStatus {{
{chr(10).join(f'    /** {e["description"]} */{chr(10)}    {e["code"]},' for e in b[:-1])}
    /** {b[-1]["description"]} */
    {b[-1]["code"]};

    /** Parse a wire value; unknown input must never read as APPROVED. */
    public static TransactionStatus fromWire(String raw) {{
        if (raw == null) return FAILED;
        try {{
            return valueOf(raw.trim().toUpperCase());
        }} catch (IllegalArgumentException e) {{
            return FAILED;
        }}
    }}

    /** PENDING or RUNNING — a genuine grouping, not an alias for the status. */
    public boolean isSettling() {{
        return this == PENDING || this == RUNNING;
    }}
}}
""")

# --- RequestAction ----------------------------------------------------------
a = A["A_serviceRequestTypes"]
(PKG / "RequestAction.java").write_text(HEADER + f"""
/** Appendix A — every REQUEST_ACTION the gateway accepts. */
public enum RequestAction {{
{chr(10).join(f'    {e["code"]},' for e in a[:-1])}
    {a[-1]["code"]};

    public String wire() {{
        return name();
    }}
}}
""")

# --- ServiceResponseCodes ---------------------------------------------------
d = A["D_serviceResponseCodes"]
(PKG / "ServiceResponseCodes.java").write_text(HEADER + f"""
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Appendix D — service response codes and the decline taxonomy. */
public final class ServiceResponseCodes {{

    /** Metadata for a single service response code. */
    public static final class Info {{
        private final int code;
        private final String description;
        private final boolean retryable;
        private final boolean stopRecurring;
        private final boolean approval;
        private final boolean terminal;

        Info(int code, String description, boolean retryable, boolean stopRecurring,
             boolean approval, boolean terminal) {{
            this.code = code;
            this.description = description;
            this.retryable = retryable;
            this.stopRecurring = stopRecurring;
            this.approval = approval;
            this.terminal = terminal;
        }}

        public int code() {{ return code; }}
        public String description() {{ return description; }}
        /** Transient — a retry may succeed. Dunning logic branches on this. */
        public boolean retryable() {{ return retryable; }}
        /** Hard stop for recurring/card-on-file billing. */
        public boolean stopRecurring() {{ return stopRecurring; }}
        public boolean approval() {{ return approval; }}
        /** Neither approval nor retryable — do not retry. */
        public boolean terminal() {{ return terminal; }}
    }}

    private static final Map<Integer, Info> BY_CODE;

    static {{
        Map<Integer, Info> m = new HashMap<>();
{chr(10).join(f'        m.put({e["code"]}, new Info({e["code"]}, {j(e["description"])}, {str(e["retryable"]).lower()}, {str(e["stopRecurring"]).lower()}, {str(e["approval"]).lower()}, {str(e["terminal"]).lower()}));' for e in d)}
        BY_CODE = Collections.unmodifiableMap(m);
    }}

    private ServiceResponseCodes() {{}}

    public static Info get(int code) {{ return BY_CODE.get(code); }}
    public static Map<Integer, Info> all() {{ return BY_CODE; }}
}}
""")

# --- ApiResponseCodes -------------------------------------------------------
c = A["C_apiResponseCodes"]
(PKG / "ApiResponseCodes.java").write_text(HEADER + f"""
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Appendix C — gateway request-validation codes (fire before the processor). */
public final class ApiResponseCodes {{

    public static final class Info {{
        private final int code;
        private final String description;
        private final String recommendation;
        private final String mapsToException;
        private final boolean carriesRefField;

        Info(int code, String description, String recommendation,
             String mapsToException, boolean carriesRefField) {{
            this.code = code;
            this.description = description;
            this.recommendation = recommendation;
            this.mapsToException = mapsToException;
            this.carriesRefField = carriesRefField;
        }}

        public int code() {{ return code; }}
        public String description() {{ return description; }}
        public String recommendation() {{ return recommendation; }}
        /** Which SDK exception this code raises (object model §3.7). */
        public String mapsToException() {{ return mapsToException; }}
        public boolean carriesRefField() {{ return carriesRefField; }}
    }}

    private static final Map<Integer, Info> BY_CODE;

    static {{
        Map<Integer, Info> m = new HashMap<>();
{chr(10).join(f'        m.put({e["code"]}, new Info({e["code"]}, {j(e["description"])}, {j(e["recommendation"])}, {j(e["mapsToException"])}, {str(e["carriesRefField"]).lower()}));' for e in c)}
        BY_CODE = Collections.unmodifiableMap(m);
    }}

    private ApiResponseCodes() {{}}

    public static Info get(int code) {{ return BY_CODE.get(code); }}
    public static Map<Integer, Info> all() {{ return BY_CODE; }}
}}
""")

# --- AvsCodes / CvvCodes ----------------------------------------------------
e_ = A["E_avsCodes"]
(PKG / "AvsCodes.java").write_text(HEADER + f"""
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Appendix E — AVS codes. {{@code classification}} is DERIVED, not from the spec. */
public final class AvsCodes {{

    public static final class Info {{
        private final String code;
        private final String description;
        private final String cardNetwork;
        private final String classification;

        Info(String code, String description, String cardNetwork, String classification) {{
            this.code = code;
            this.description = description;
            this.cardNetwork = cardNetwork;
            this.classification = classification;
        }}

        public String code() {{ return code; }}
        public String description() {{ return description; }}
        public String cardNetwork() {{ return cardNetwork; }}
        /**
         * DERIVED: positive | partial | negative | neutral. {{@code partial}} means
         * some elements matched and some did not (e.g. street matches, postal
         * does not). Whether a partial is acceptable is a merchant risk-policy
         * decision — the SDK reports it and does not decide.
         */
        public String classification() {{ return classification; }}
    }}

    private static final Map<String, Info> BY_CODE;

    static {{
        Map<String, Info> m = new HashMap<>();
{chr(10).join(f'        m.put({j(x["code"])}, new Info({j(x["code"])}, {j(x["description"])}, {j(x["cardNetwork"])}, {j(x["classification"])}));' for x in e_)}
        BY_CODE = Collections.unmodifiableMap(m);
    }}

    private AvsCodes() {{}}

    public static Info get(String code) {{
        return code == null ? null : BY_CODE.get(code.trim().toUpperCase());
    }}
    public static Map<String, Info> all() {{ return BY_CODE; }}
}}
""")

f_ = A["F_cvvCodes"]
(PKG / "CvvCodes.java").write_text(HEADER + f"""
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Appendix F — CVV codes. {{@code classification}} is DERIVED, not from the spec. */
public final class CvvCodes {{

    public static final class Info {{
        private final String code;
        private final String description;
        private final String classification;

        Info(String code, String description, String classification) {{
            this.code = code;
            this.description = description;
            this.classification = classification;
        }}

        public String code() {{ return code; }}
        public String description() {{ return description; }}
        /** DERIVED: match | no_match | neutral. */
        public String classification() {{ return classification; }}
    }}

    private static final Map<String, Info> BY_CODE;

    static {{
        Map<String, Info> m = new HashMap<>();
{chr(10).join(f'        m.put({j(x["code"])}, new Info({j(x["code"])}, {j(x["description"])}, {j(x["classification"])}));' for x in f_)}
        BY_CODE = Collections.unmodifiableMap(m);
    }}

    private CvvCodes() {{}}

    public static Info get(String code) {{
        return code == null ? null : BY_CODE.get(code.trim().toUpperCase());
    }}
    public static Map<String, Info> all() {{ return BY_CODE; }}
}}
""")

(PKG / "SpecVersion.java").write_text(HEADER + f"""
/** The spec revision these enums were generated from. */
public final class SpecVersion {{
    public static final String API_VERSION = {j(ver)};

    private SpecVersion() {{}}
}}
""")

n = sum(len(v) for v in A.values())
print(f"generated {PKG} ({n} enum values from spec v{ver})")
