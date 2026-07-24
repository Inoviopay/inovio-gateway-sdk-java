package com.inoviopay.gateway.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared request building blocks (object model §3.3).
 *
 * <p>Grouped as static nested classes so the flat, uppercase wire params
 * (CUST_*, BILL_ADDR_*, LI_*_n ...) are produced from cohesive objects and the
 * partner never types a wire field name. These would be {@code record}s on
 * Java 17+; the Java 11 baseline uses mutable builders-by-setter for brevity.
 */
public final class RequestParts {

    private RequestParts() {}

    /** CUST_* + XTL_IP */
    public static final class Customer {
        public String firstName, lastName, email, phone, login, password;
        /** MM-DD-YYYY per spec §4.2 */
        public String birthday;
        public String dln, dlnState, ssnLast4;
        /** Brazil CPF/CNPJ — presence activates Credilink scrubbing. */
        public String brCpfCnpj;
        public String ip, userAgent;
    }

    /** BILL_ADDR_* / SHIP_ADDR_* */
    public static final class Address {
        public String line1, line2, city, state, zip;
        /** ISO-2 */
        public String country;
        public String district;
    }

    /** LI_*_n — the SDK owns the 1-based wire indexing. */
    public static final class LineItem {
        public final String productId;
        public final int count;
        public final Money value;
        public String xtlProductId;
        public String type;

        public LineItem(String productId, int count, Money value) {
            this.productId = productId;
            this.count = count;
            this.value = value;
        }
    }

    /** PMT_DESCRIPTOR* */
    public static final class Descriptor {
        public String name, phone, city;

        public Descriptor(String name) { this.name = name; }
    }

    /** Spec §14.3 — opt-in, NOT defaulted on (Q6). Range 30..600 seconds. */
    public static final class TimeoutVoid {
        public final int seconds;

        public TimeoutVoid(int seconds) { this.seconds = seconds; }
    }

    /** CHKAVS / CHKCVV / REQUEST_MAX_WAIT */
    public static final class RiskOptions {
        /** on | off | ignore | conditional */
        public String avs;
        public String avsMatchSet;
        public String cvv;
        public String cvvMatchSet;
        public TimeoutVoid timeoutVoid;
    }

    /** PARTIAL_AUTH / PARTIAL_AUTH_MIN */
    public static final class PartialAuth {
        public boolean enabled;
        public Money minimumAmount;

        public PartialAuth(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * Idempotency (Q6). {@code mode} maps to UNIQUE_XTL_ORDER_ID and defaults to
     * RETURN_ORIGINAL — a retry returns the original result instead of charging twice.
     */
    public static final class Idempotency {
        public final String xtlOrderId;
        /** OFF | DECLINE_DUP | RETURN_ORIGINAL */
        public String mode;

        public Idempotency(String xtlOrderId) { this.xtlOrderId = xtlOrderId; }
    }

    /** Card-on-file / recurring compliance flags (Appendices G/J/K). */
    public static final class Recurring {
        /** CIT | MIT */
        public String initiator;
        /** NONE | REBILL | START_SUBSCRIPTION */
        public String rebill;
        /** NONE | TRIAL | INITIAL | REBILL */
        public String rebillType;
        public Boolean installment, cardOnFile, trialConsent;
        public String membershipXtlId, receipt;
    }

    public static final class Tax {
        public final Money amount;
        public Boolean exempt;

        public Tax(Money amount) { this.amount = amount; }
    }

    public static final class Fees {
        public Tax tax;
        public Money convenienceFee;
    }

    public static final class Affiliate {
        public String affId, subAffId;
    }

    /** XTL_UDF01..20, TPPE_ID, PROC_UDF01/02 */
    public static final class Metadata {
        public final Map<String, String> udf = new LinkedHashMap<>();
        public String tppeId, procUdf1, procUdf2;
    }

    /** 3DS — the gateway silently disables 3DS if any of these is missing. */
    public static final class BrowserData {
        public final String language, userAgent, header;

        public BrowserData(String language, String userAgent, String header) {
            this.language = language;
            this.userAgent = userAgent;
            this.header = header;
        }
    }
}
