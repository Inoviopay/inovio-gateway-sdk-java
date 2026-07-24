// GENERATED FILE — DO NOT EDIT.
// Source: Inovio Gateway Payments Service API v4.14 (api-sdk/spec/spec-enums.json)
// Regenerate: python3 scripts/generate_enums.py
//
// Classifiers (retryable/terminal/stopRecurring, AVS/CVV classification and the
// API-code -> exception mapping) are DERIVED by the SDK project, not stated in
// the spec. See api-sdk/spec/README.md.
package com.inoviopay.gateway.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Appendix C — gateway request-validation codes (fire before the processor). */
public final class ApiResponseCodes {

    public static final class Info {
        private final int code;
        private final String description;
        private final String recommendation;
        private final String mapsToException;
        private final boolean carriesRefField;

        Info(int code, String description, String recommendation,
             String mapsToException, boolean carriesRefField) {
            this.code = code;
            this.description = description;
            this.recommendation = recommendation;
            this.mapsToException = mapsToException;
            this.carriesRefField = carriesRefField;
        }

        public int code() { return code; }
        public String description() { return description; }
        public String recommendation() { return recommendation; }
        /** Which SDK exception this code raises (object model §3.7). */
        public String mapsToException() { return mapsToException; }
        public boolean carriesRefField() { return carriesRefField; }
    }

    private static final Map<Integer, Info> BY_CODE;

    static {
        Map<Integer, Info> m = new HashMap<>();
        m.put(100, new Info(100, "Invalid login information (throttle)", "Check your login credentials and try again. If you continue to receive this response, contact Client Support", "RateLimitException", false));
        m.put(101, new Info(101, "Invalid login information", "Check your login credentials and try again. If you continue to receive this response, contact Client Support", "AuthenticationException", false));
        m.put(102, new Info(102, "User not active", "These credentials have been disabled. If you think this is an error, contact Client Support", "AuthenticationException", false));
        m.put(103, new Info(103, "Invalid site", "The value of SITE_ID does not exist, or it does not match the authentication credentials provided.", "AuthenticationException", false));
        m.put(104, new Info(104, "Invalid service", "Check the value of request_action to confirm it is correct.", "AuthenticationException", false));
        m.put(105, new Info(105, "Invalid service action", "Check the value of request_action to confirm it is correct.", "AuthenticationException", false));
        m.put(106, new Info(106, "Invalid service object", "Check the value of request_object to confirm it is correct.", "AuthenticationException", false));
        m.put(110, new Info(110, "Required field", "A required key/value pair has not been included in the request. In the response, check the value of REF_FIELD to see what is missing", "ValidationException", true));
        m.put(111, new Info(111, "Invalid length", "The length of a value is too short or long. Check the returned value of REF_FIELD to see which field may need editing", "ValidationException", true));
        m.put(112, new Info(112, "Not numeric", "Numeric data is expected. Confirm the amount sent for LI_VALUE_x, which should only contain numerals and one decimal Something in the request was not", "ValidationException", false));
        m.put(113, new Info(113, "Invalid Data", "expected. Check the values that were submitted for unusual characters, spaces, or null values where there perhaps should not be", "ValidationException", false));
        m.put(115, new Info(115, "Customer not found", "If CUST_ID or CUST_ID_XTL was submitted, check these values and try again. If this response has come from a request without these parameters, contact Client Support", "ValidationException", false));
        m.put(116, new Info(116, "User MUST change password", "User passwords expire every 90 days. This does not apply to API credentials.", "ValidationException", false));
        m.put(118, new Info(118, "New password must not match the previous 5 passwords", "Try a different password.", "ValidationException", false));
        m.put(119, new Info(119, "request_ref_po_id and request_po_li_id mismatch", "The order ID and the line item ID do not relate to one another. Check the order information.", "ValidationException", false));
        m.put(120, new Info(120, "System Error", "Contact Client Support", "ValidationException", false));
        m.put(125, new Info(125, "Duplicate Login", "This email address, a unique identifier, already exists.", "ConfigurationException", false));
        m.put(130, new Info(130, "Same Product ID found on different line items.", "Check the values of LI_PROD_ID_x. Each one should have a unique ID. If the intent is to submit a purchase for multiples of the same product use LI_COUNT_x to indicate the quantity.", "ConfigurationException", false));
        m.put(135, new Info(135, "Duplicate Company Name", "This company name is already in the system. If you are certain it doesn't already exist in the system, it could be a company with the same name, but doing business in a different region. Contact Client Support for assistance.", "ConfigurationException", false));
        m.put(136, new Info(136, "Duplicate Site Name", "This site name already exists in our system.", "ConfigurationException", false));
        m.put(150, new Info(150, "Product Not Found", "The product ID is not valid. It may not exist, or it might be associated with another site. Check", "ConfigurationException", false));
        m.put(152, new Info(152, "Product Type Not Found", "The value for PROD_TYPE is not valid.", "ConfigurationException", false));
        m.put(153, new Info(153, "Duplicate XTL product id", "This value is already in the system. To confirm and review, the ID can be searched for in our", "ConfigurationException", false));
        m.put(155, new Info(155, "Selected currency not configured", "Check the merchant account configuration in the portal.", "ConfigurationException", false));
        m.put(160, new Info(160, "Invalid product amount", "Check the value of LI_VALUE_x to confirm it is the intended amount.", "ConfigurationException", false));
        m.put(165, new Info(165, "Currency not supported", "Check the merchant account configuration in the portal. The MID's allowed currencies can be configured there. Additionally, check the value of PROCESSOR_RESPONSE in the", "ConfigurationException", false));
        m.put(170, new Info(170, "Duplicate product amount and currency", "A product with matching properties already exists within the site.", "ConfigurationException", false));
        m.put(176, new Info(176, "Duplicate product description and language", "A product with matching properties already exists within this Site", "ConfigurationException", false));
        m.put(180, new Info(180, "Invalid transaction limit type", "The limit type was not recognized. Try using the portal to adjust velocity settings.", "ConfigurationException", false));
        m.put(181, new Info(181, "Invalid limit type", "The limit type was not recognized. Try using the portal to adjust velocity settings.", "ConfigurationException", false));
        m.put(183, new Info(183, "Payment Type is required", "Confirm that PMT_TYPE has been submitted, and has not been included multiple times.", "ConfigurationException", false));
        m.put(205, new Info(205, "No Permissions on requested object", "You may not be able to check and confirm your own user permissions, so it may be necessary for an administrator to check them for you. If", "ConfigurationException", false));
        m.put(210, new Info(210, "Merchant Account not found", "you feel this is an error, contact your administrator or Client Support. Verify the value of MERCH_ACCT_ID", "ConfigurationException", false));
        m.put(211, new Info(211, "Currency not found", "The expected format is three-character currency code.", "ConfigurationException", false));
        m.put(215, new Info(215, "Invalid Card Brand", "Check the card brand submitted. If you are certain it\u2019s correct, contact Client Support", "ConfigurationException", false));
        m.put(410, new Info(410, "Field not supported with wallet payment", "Check the value of REF_FIELD in the response to see what incompatible element was", "ConfigurationException", true));
        m.put(411, new Info(411, "REQUEST_CURRENCY mismatch with Cryptogram", "The currency in the gateway request needs to match the currency that was packed into the ApplePay cryptogram", "ConfigurationException", false));
        m.put(414, new Info(414, "GooglePay token has expired", "", "ConfigurationException", false));
        BY_CODE = Collections.unmodifiableMap(m);
    }

    private ApiResponseCodes() {}

    public static Info get(int code) { return BY_CODE.get(code); }
    public static Map<Integer, Info> all() { return BY_CODE; }
}
