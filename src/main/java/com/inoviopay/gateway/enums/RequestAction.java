// GENERATED FILE — DO NOT EDIT.
// Source: Inovio Gateway Payments Service API v4.14 (api-sdk/spec/spec-enums.json)
// Regenerate: python3 scripts/generate_enums.py
//
// Classifiers (retryable/terminal/stopRecurring, AVS/CVV classification and the
// API-code -> exception mapping) are DERIVED by the SDK project, not stated in
// the spec. See api-sdk/spec/README.md.
package com.inoviopay.gateway.enums;

/** Appendix A — every REQUEST_ACTION the gateway accepts. */
public enum RequestAction {
    ACHAUTHCAP,
    ACHAUTHORIZE,
    ACHREVERSE,
    ACHCREDIT,
    APPLEPAYCONFIG,
    CCAUTHORIZE,
    CCCAPTURE,
    CCAUTHCAP,
    CCREVERSE,
    CCREVERSECAP,
    CCCREDIT,
    CCRDR,
    CCRDRDELETE,
    CCTC40,
    CCSTATUS,
    CCTRANSUPDATE,
    DBTAUTHORIZE,
    DBTCAPTURE,
    DBTCREDIT,
    DBTDEBIT,
    DBTREVERSE,
    GOOGLEPAYCONFIG,
    TESTGW,
    TESTAUTH,
    SUB_CANCEL,
    SUB_UPDATE,
    BOLETOAUTHCAP,
    PIXSALE,
    PAGSALE;

    public String wire() {
        return name();
    }
}
