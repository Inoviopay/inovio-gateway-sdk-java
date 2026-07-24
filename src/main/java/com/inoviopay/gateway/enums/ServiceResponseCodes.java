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

/** Appendix D — service response codes and the decline taxonomy. */
public final class ServiceResponseCodes {

    /** Metadata for a single service response code. */
    public static final class Info {
        private final int code;
        private final String description;
        private final boolean retryable;
        private final boolean stopRecurring;
        private final boolean approval;
        private final boolean terminal;

        Info(int code, String description, boolean retryable, boolean stopRecurring,
             boolean approval, boolean terminal) {
            this.code = code;
            this.description = description;
            this.retryable = retryable;
            this.stopRecurring = stopRecurring;
            this.approval = approval;
            this.terminal = terminal;
        }

        public int code() { return code; }
        public String description() { return description; }
        /** Transient — a retry may succeed. Dunning logic branches on this. */
        public boolean retryable() { return retryable; }
        /** Hard stop for recurring/card-on-file billing. */
        public boolean stopRecurring() { return stopRecurring; }
        public boolean approval() { return approval; }
        /** Neither approval nor retryable — do not retry. */
        public boolean terminal() { return terminal; }
    }

    private static final Map<Integer, Info> BY_CODE;

    static {
        Map<Integer, Info> m = new HashMap<>();
        m.put(100, new Info(100, "User Authorized", false, false, true, false));
        m.put(101, new Info(101, "Service Available", false, false, true, false));
        m.put(102, new Info(102, "Membership Updated", false, false, true, false));
        m.put(150, new Info(150, "Product Not Found", false, false, false, true));
        m.put(152, new Info(152, "Product Type Not Found", false, false, false, true));
        m.put(155, new Info(155, "Selected currency not configured", false, false, false, true));
        m.put(157, new Info(157, "MID has RDR Status OFF", false, false, false, true));
        m.put(190, new Info(190, "Invalid Product Configuration", false, false, false, true));
        m.put(192, new Info(192, "Product Not Active", false, false, false, true));
        m.put(200, new Info(200, "CVV required by processor", false, false, false, true));
        m.put(201, new Info(201, "Country required by processor", false, false, false, true));
        m.put(202, new Info(202, "DOB required by processor", false, false, false, true));
        m.put(203, new Info(203, "SSN required by processor", false, false, false, true));
        m.put(204, new Info(204, "Address required by processor", false, false, false, true));
        m.put(205, new Info(205, "City required by processor", false, false, false, true));
        m.put(206, new Info(206, "State required by processor", false, false, false, true));
        m.put(207, new Info(207, "Postal Code required by processor", false, false, false, true));
        m.put(208, new Info(208, "Phone required by processor", false, false, false, true));
        m.put(209, new Info(209, "IP required by processor", false, false, false, true));
        m.put(210, new Info(210, "CPF required by processor", false, false, false, true));
        m.put(211, new Info(211, "Email required by processor", false, false, false, true));
        m.put(212, new Info(212, "FName required by processor", false, false, false, true));
        m.put(213, new Info(213, "LName required by processor", false, false, false, true));
        m.put(215, new Info(215, "Activity limit exceeded", false, false, false, true));
        m.put(216, new Info(216, "Invalid amount", false, false, false, true));
        m.put(217, new Info(217, "No such issuer", false, false, false, true));
        m.put(218, new Info(218, "Wrong PIN entered", false, false, false, true));
        m.put(219, new Info(219, "R0: Stop recurring payments", false, true, false, true));
        m.put(220, new Info(220, "R1: Stop recurring payments", false, true, false, true));
        m.put(221, new Info(221, "System malfunction", false, false, false, true));
        m.put(500, new Info(500, "No merchant account configured", false, false, false, true));
        m.put(501, new Info(501, "Customer not found", false, false, false, true));
        m.put(502, new Info(502, "Transaction error", false, false, false, true));
        m.put(503, new Info(503, "Service Unavailable", false, false, false, true));
        m.put(505, new Info(505, "Order adjusted to zero", false, false, false, true));
        m.put(506, new Info(506, "Capture amount exceeds order value", false, false, false, true));
        m.put(507, new Info(507, "Order fully captured", false, false, false, true));
        m.put(510, new Info(510, "Order already reversed", false, false, false, true));
        m.put(511, new Info(511, "Order already charged back", false, false, false, true));
        m.put(512, new Info(512, "Order not found", false, false, false, true));
        m.put(515, new Info(515, "Order fully credited", false, false, false, true));
        m.put(516, new Info(516, "Credit amount exceeds order value", false, false, false, true));
        m.put(518, new Info(518, "Missing required field", false, false, false, true));
        m.put(520, new Info(520, "Unsupported Currency", false, false, false, true));
        m.put(522, new Info(522, "Unsupported card brand", false, false, false, true));
        m.put(525, new Info(525, "Batch Closed: Please credit", false, false, false, true));
        m.put(526, new Info(526, "ApplePay is not supported on this merch_acct_id", false, false, false, true));
        m.put(527, new Info(527, "No ApplePay merch_acct_id configured", false, false, false, true));
        m.put(528, new Info(528, "ApplePay MCC Restricted", false, false, false, true));
        m.put(530, new Info(530, "Downstream Processor Unavailable", false, false, false, true));
        m.put(536, new Info(536, "Order not settled: Please reverse", false, false, false, true));
        m.put(540, new Info(540, "Maximum Auth Limit Exceeded", false, false, false, true));
        m.put(546, new Info(546, "GooglePay MCC Restricted", false, false, false, true));
        m.put(547, new Info(547, "No GooglePay merch_acct_id configured", false, false, false, true));
        m.put(548, new Info(548, "GooglePay is not supported on this merch_acct_id", false, false, false, true));
        m.put(555, new Info(555, "Call Center", false, false, false, true));
        m.put(560, new Info(560, "Invalid Service Action", false, false, false, true));
        m.put(564, new Info(564, "Invalid Terminal", false, false, false, true));
        m.put(565, new Info(565, "Invalid Amount", false, false, false, true));
        m.put(570, new Info(570, "Invalid Card Type", false, false, false, true));
        m.put(580, new Info(580, "Unsupported Request", false, false, false, true));
        m.put(600, new Info(600, "Declined", false, false, false, true));
        m.put(601, new Info(601, "Scrub Decline", false, false, false, true));
        m.put(603, new Info(603, "Fraud", false, false, false, true));
        m.put(605, new Info(605, "Stolen Card", false, false, false, true));
        m.put(610, new Info(610, "Pickup Card", false, false, false, true));
        m.put(615, new Info(615, "Lost Card", false, false, false, true));
        m.put(620, new Info(620, "Invalid CVV", false, false, false, true));
        m.put(621, new Info(621, "Failed CVV", false, false, false, true));
        m.put(622, new Info(622, "Invalid AVS", false, false, false, true));
        m.put(623, new Info(623, "Failed AVS", false, false, false, true));
        m.put(624, new Info(624, "Expired Card", false, false, false, true));
        m.put(625, new Info(625, "Excessive Use", false, false, false, true));
        m.put(630, new Info(630, "Invalid Card Number", false, false, false, true));
        m.put(635, new Info(635, "Insufficient Funds", true, false, false, false));
        m.put(640, new Info(640, "Retry", true, false, false, false));
        m.put(650, new Info(650, "Do Not Honor", false, false, false, true));
        m.put(660, new Info(660, "Partial Approval", true, false, false, false));
        m.put(670, new Info(670, "Additional Authentication Required", false, false, false, true));
        m.put(675, new Info(675, "Invalid Card Number, failed Mod 10 validation", false, false, false, true));
        m.put(680, new Info(680, "Duplicate Transaction Detected", false, false, false, true));
        m.put(685, new Info(685, "Duplicate Order Detected", false, false, false, true));
        m.put(690, new Info(690, "Active Membership Exists", false, false, false, true));
        m.put(692, new Info(692, "Invalid Rebill Product", false, false, false, true));
        m.put(695, new Info(695, "Site Username Unavailable", false, false, false, true));
        m.put(697, new Info(697, "Membership Not Active", false, false, false, true));
        m.put(698, new Info(698, "Membership Not Found", false, false, false, true));
        m.put(699, new Info(699, "Membership Not Set for Rebill", false, false, false, true));
        m.put(700, new Info(700, "Scrub Decline", false, false, false, true));
        m.put(706, new Info(706, "Failed Age Validation", false, false, false, true));
        m.put(707, new Info(707, "Invalid CPF", false, false, false, true));
        BY_CODE = Collections.unmodifiableMap(m);
    }

    private ServiceResponseCodes() {}

    public static Info get(int code) { return BY_CODE.get(code); }
    public static Map<Integer, Info> all() { return BY_CODE; }
}
