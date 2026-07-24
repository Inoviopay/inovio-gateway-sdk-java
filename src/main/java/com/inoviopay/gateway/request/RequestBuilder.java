package com.inoviopay.gateway.request;

import com.inoviopay.gateway.errors.ValidationException;
import com.inoviopay.gateway.model.Card;
import com.inoviopay.gateway.model.PaymentMethods;
import com.inoviopay.gateway.model.RequestParts.Address;
import com.inoviopay.gateway.model.RequestParts.Customer;
import com.inoviopay.gateway.model.RequestParts.LineItem;
import com.inoviopay.gateway.model.SavedCard;
import com.inoviopay.gateway.model.Token;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model -&gt; wire projection.
 *
 * <p>This is where the SDK earns its keep: flat, uppercase, 1-indexed wire
 * params (LI_VALUE_1, BILL_ADDR_ZIP, REQUEST_INITATOR ...) are produced from
 * cohesive objects so the partner never types a wire field name.
 */
public final class RequestBuilder {

    private static final Map<String, String> IDEMPOTENCY_WIRE = new HashMap<>();
    private static final Map<String, String> AVS_WIRE = new HashMap<>();
    private static final Map<String, String> REBILL_WIRE = new HashMap<>();
    private static final Map<String, String> REBILL_TYPE_WIRE = new HashMap<>();

    static {
        IDEMPOTENCY_WIRE.put("OFF", "0");
        IDEMPOTENCY_WIRE.put("DECLINE_DUP", "1");
        IDEMPOTENCY_WIRE.put("RETURN_ORIGINAL", "2");
        AVS_WIRE.put("on", "1");
        AVS_WIRE.put("off", "0");
        AVS_WIRE.put("ignore", "2");
        AVS_WIRE.put("conditional", "3");
        REBILL_WIRE.put("NONE", "0");
        REBILL_WIRE.put("REBILL", "1");
        REBILL_WIRE.put("START_SUBSCRIPTION", "2");
        REBILL_TYPE_WIRE.put("NONE", "0");
        REBILL_TYPE_WIRE.put("TRIAL", "1");
        REBILL_TYPE_WIRE.put("INITIAL", "2");
        REBILL_TYPE_WIRE.put("REBILL", "3");
    }

    private RequestBuilder() {}

    public static Map<String, String> build(TransactionRequest req) {
        Map<String, String> p = new LinkedHashMap<>();
        PaymentMethods.assertV1(req.paymentMethod);

        if (req.lineItems.isEmpty()) {
            throw new ValidationException("at least one line item is required", "LI_VALUE_1");
        }

        // --- payment method: absorbs the PMT_NUMB overload ---
        if (req.paymentMethod instanceof Card) {
            Card c = (Card) req.paymentMethod;
            put(p, "PMT_NUMB", c.number());
            put(p, "PMT_EXPIRY", c.expiry());
            put(p, "PMT_KEY", c.cvv());
        } else if (req.paymentMethod instanceof Token) {
            put(p, "TOKEN_GUID", ((Token) req.paymentMethod).guid());
        } else if (req.paymentMethod instanceof SavedCard) {
            SavedCard s = (SavedCard) req.paymentMethod;
            put(p, "PMT_ID", s.pmtId());
            put(p, "PMT_ID_XTL", s.pmtIdXtl());
            put(p, "CUST_ID", s.custId());
        }

        // --- line items: the SDK owns the 1-based wire indexing ---
        String currency = req.amount == null ? null : req.amount.currency();
        for (int i = 0; i < req.lineItems.size(); i++) {
            LineItem li = req.lineItems.get(i);
            int n = i + 1;
            if (li.count > 10) {
                throw new ValidationException(
                    "line item " + n + ": count must be <= 10 (spec §4.4)", "LI_COUNT_" + n);
            }
            put(p, "LI_PROD_ID_" + n, li.productId);
            put(p, "LI_PROD_ID_XTL_" + n, li.xtlProductId);
            put(p, "LI_COUNT_" + n, String.valueOf(li.count));
            put(p, "LI_VALUE_" + n, li.value.toWire());
            put(p, "LI_TYPE_" + n, li.type);
            if (currency == null) {
                currency = li.value.currency();
            } else if (!currency.equals(li.value.currency())) {
                throw new ValidationException(
                    "line item " + n + " currency " + li.value.currency()
                        + " does not match " + currency
                        + " — a single transaction cannot mix currencies");
            }
        }
        put(p, "REQUEST_CURRENCY", currency);

        Customer c = req.customer;
        if (c != null) {
            put(p, "CUST_FNAME", c.firstName);
            put(p, "CUST_LNAME", c.lastName);
            put(p, "CUST_EMAIL", c.email);
            put(p, "CUST_PHONE", c.phone);
            put(p, "CUST_LOGIN", c.login);
            put(p, "CUST_PASSWORD", c.password);
            put(p, "CUST_BIRTHDAY", c.birthday);
            put(p, "CUST_DLN", c.dln);
            put(p, "CUST_DLN_STATE", c.dlnState);
            put(p, "CUST_SSN_L4", c.ssnLast4);
            put(p, "CUST_BRCPFCNPJ", c.brCpfCnpj);
            put(p, "XTL_IP", c.ip);
            put(p, "USER_AGENT_XTL", c.userAgent);
        }
        address(p, "BILL", req.billingAddress);
        address(p, "SHIP", req.shippingAddress);

        if (req.descriptor != null) {
            put(p, "PMT_DESCRIPTOR", req.descriptor.name);
            put(p, "PMT_DESCRIPTOR_PHONE", req.descriptor.phone);
            put(p, "PMT_DESCRIPTOR_CITY", req.descriptor.city);
        }

        if (req.risk != null) {
            if (req.risk.avs != null) put(p, "CHKAVS", AVS_WIRE.get(req.risk.avs));
            put(p, "AVS_MATCH_SET", req.risk.avsMatchSet);
            if (req.risk.cvv != null) put(p, "CHKCVV", AVS_WIRE.get(req.risk.cvv));
            put(p, "CVV_MATCH_SET", req.risk.cvvMatchSet);
            if (req.risk.timeoutVoid != null) {
                int s = req.risk.timeoutVoid.seconds;
                if (s < 30 || s > 600) {
                    throw new ValidationException(
                        "risk.timeoutVoid.seconds must be between 30 and 600, got " + s,
                        "REQUEST_MAX_WAIT");
                }
                put(p, "REQUEST_MAX_WAIT", String.valueOf(s));
            }
        }

        if (req.partialAuth != null && req.partialAuth.enabled) {
            put(p, "PARTIAL_AUTH", "1");
            if (req.partialAuth.minimumAmount != null) {
                put(p, "PARTIAL_AUTH_MIN", req.partialAuth.minimumAmount.toWire());
            }
        }

        if (req.idempotency != null) {
            put(p, "XTL_ORDER_ID", req.idempotency.xtlOrderId);
            String mode = req.idempotency.mode == null ? "RETURN_ORIGINAL" : req.idempotency.mode;
            put(p, "UNIQUE_XTL_ORDER_ID", IDEMPOTENCY_WIRE.get(mode));
        }

        if (req.recurring != null) {
            // NOTE: the wire field is misspelled "INITATOR". Normalized here so
            // the partner never sees it.
            put(p, "REQUEST_INITATOR", req.recurring.initiator);
            if (req.recurring.rebill != null) {
                put(p, "REQUEST_REBILL", REBILL_WIRE.get(req.recurring.rebill));
            }
            if (req.recurring.rebillType != null) {
                put(p, "TRANS_REBILL_TYPE", REBILL_TYPE_WIRE.get(req.recurring.rebillType));
            }
            flag(p, "INSTALLMENT", req.recurring.installment);
            flag(p, "CARD_ON_FILE", req.recurring.cardOnFile);
            put(p, "MBSHP_ID_XTL", req.recurring.membershipXtlId);
            flag(p, "TRIAL_CONSENT", req.recurring.trialConsent);
            put(p, "RECEIPT", req.recurring.receipt);
        }

        if (req.fees != null) {
            if (req.fees.tax != null) {
                put(p, "TAX_AMT", req.fees.tax.amount.toWire());
                flag(p, "TAX_EXEMPT", req.fees.tax.exempt);
            }
            if (req.fees.convenienceFee != null) {
                put(p, "CONVENIENCE_FEE", req.fees.convenienceFee.toWire());
            }
        }

        if (req.affiliate != null) {
            put(p, "REQUEST_AFF_ID", req.affiliate.affId);
            put(p, "REQUEST_AFF_ID_SUB", req.affiliate.subAffId);
        }

        if (req.metadata != null) {
            put(p, "TPPE_ID", req.metadata.tppeId);
            put(p, "PROC_UDF01", req.metadata.procUdf1);
            put(p, "PROC_UDF02", req.metadata.procUdf2);
            for (Map.Entry<String, String> e : req.metadata.udf.entrySet()) {
                String k = e.getKey();
                put(p, "XTL_UDF" + (k.length() < 2 ? "0" + k : k), e.getValue());
            }
        }

        if (req.browser != null) {
            put(p, "P3DS_BROWSER_LANGUAGE", req.browser.language);
            put(p, "USER_AGENT_XTL", req.browser.userAgent);
            put(p, "P3DS_BROWSER_HEADER", req.browser.header);
        }

        put(p, "MERCH_ACCT_ID", req.merchAcctId);
        if (req.forceCredit) put(p, "FORCE_CREDIT", "1");
        return p;
    }

    private static void address(Map<String, String> p, String prefix, Address a) {
        if (a == null) return;
        put(p, prefix + "_ADDR", a.line1);
        put(p, prefix + "_ADDR2", a.line2);
        put(p, prefix + "_ADDR_CITY", a.city);
        put(p, prefix + "_ADDR_STATE", a.state);
        put(p, prefix + "_ADDR_ZIP", a.zip);
        put(p, prefix + "_ADDR_COUNTRY", a.country);
        put(p, prefix + "_ADDR_DISTRICT", a.district);
    }

    private static void put(Map<String, String> p, String k, String v) {
        if (v != null && !v.isEmpty()) p.put(k, v);
    }

    private static void flag(Map<String, String> p, String k, Boolean v) {
        if (v != null) p.put(k, v ? "1" : "0");
    }
}
