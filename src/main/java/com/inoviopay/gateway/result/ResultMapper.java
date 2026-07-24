package com.inoviopay.gateway.result;

import com.inoviopay.gateway.enums.AvsCodes;
import com.inoviopay.gateway.enums.CvvCodes;
import com.inoviopay.gateway.enums.ServiceResponseCodes;
import com.inoviopay.gateway.enums.TransactionStatus;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.refs.Refs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wire response -&gt; typed result.
 *
 * <p>All the "is this approved" judgment lives here and in the generated spec
 * enums, so every language SDK classifies identically.
 */
public final class ResultMapper {

    private static final Pattern LI_ID = Pattern.compile("PO_LI_ID_(\\d+)");

    private ResultMapper() {}

    private static Integer num(String v) {
        if (v == null || v.isEmpty()) return null;
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean flag(String v) {
        return "1".equals(v) || "Y".equalsIgnoreCase(v) || "TRUE".equalsIgnoreCase(v);
    }

    /**
     * A field counts as present only when it is non-null AND non-empty.
     *
     * The gateway returns inapplicable fields as EMPTY STRINGS rather than
     * omitting them — a TESTGW response, for example, carries TRANS_ID="".
     * A plain null check therefore treats them as present and hands "" to a
     * reference constructor, which rejects it. Verified against the live T1
     * gateway; the mocked fixtures never exercised it because they omit the
     * keys entirely.
     */
    private static String val(Map<String, String> r, String key) {
        String v = r.get(key);
        return (v == null || v.isEmpty()) ? null : v;
    }

    public static TransactionResult toTransactionResult(Map<String, String> r) {
        TransactionStatus status = TransactionStatus.fromWire(r.get("TRANS_STATUS_NAME"));

        List<Refs.LineItemRef> liRefs = new ArrayList<>();
        List<String> liKeys = new ArrayList<>();
        for (String k : r.keySet()) {
            if (LI_ID.matcher(k).matches()) liKeys.add(k);
        }
        liKeys.sort(Comparator.comparingInt(k -> {
            Matcher m = LI_ID.matcher(k);
            return m.matches() ? Integer.parseInt(m.group(1)) : 0;
        }));
        for (String k : liKeys) liRefs.add(Refs.lineItem(r.get(k)));

        Money amount = null;
        if (val(r, "TRANS_VALUE") != null && val(r, "CURR_CODE_ALPHA") != null) {
            amount = Money.of(val(r, "TRANS_VALUE"), val(r, "CURR_CODE_ALPHA"));
        }

        // Conversion is reported ONLY on real FX — otherwise the "settled"
        // fields are just the auth amount echoed back and would mean nothing.
        TransactionResult.Conversion conversion = null;
        String rate = val(r, "TRANS_EXCH_RATE");
        if (rate != null && !rate.isEmpty()
            && new BigDecimal(rate).compareTo(BigDecimal.ZERO) != 0
            && val(r, "TRANS_VALUE_SETTLED") != null
            && val(r, "CURR_CODE_ALPHA_SETTLED") != null) {
            conversion = new TransactionResult.Conversion(
                Money.of(val(r, "TRANS_VALUE_SETTLED"), val(r, "CURR_CODE_ALPHA_SETTLED")), rate);
        }

        TransactionResult.Outcome outcome = new TransactionResult.Outcome(
            new TransactionResult.Tier(num(r.get("API_RESPONSE")), r.get("API_ADVICE"),
                r.get("REF_FIELD")),
            new TransactionResult.Tier(num(r.get("SERVICE_RESPONSE")), r.get("SERVICE_ADVICE")),
            new TransactionResult.Tier(num(r.get("PROCESSOR_RESPONSE")), r.get("PROCESSOR_ADVICE")),
            new TransactionResult.Tier(num(r.get("INDUSTRY_RESPONSE")), r.get("INDUSTRY_ADVICE")));

        TransactionResult.ServiceClassification svcClass = null;
        Integer svcCode = num(r.get("SERVICE_RESPONSE"));
        if (svcCode != null) {
            ServiceResponseCodes.Info info = ServiceResponseCodes.get(svcCode);
            if (info != null) {
                svcClass = new TransactionResult.ServiceClassification(
                    info.retryable(), info.stopRecurring(), info.terminal(), info.approval());
            }
        }

        AvsCodes.Info avsInfo = AvsCodes.get(r.get("AVS_RESPONSE"));
        CvvCodes.Info cvvInfo = CvvCodes.get(r.get("CVV_RESPONSE"));

        return TransactionResult.builder()
            .status(status)
            .action(r.get("REQUEST_ACTION"))
            .orderRef(val(r, "PO_ID") != null ? Refs.order(val(r, "PO_ID")) : null)
            .xtlOrderRef(val(r, "XTL_ORDER_ID") != null ? Refs.xtlOrder(val(r, "XTL_ORDER_ID")) : null)
            .transactionId(val(r, "TRANS_ID") != null ? Refs.transaction(val(r, "TRANS_ID")) : null)
            .requestId(val(r, "REQ_ID") != null ? Refs.req(val(r, "REQ_ID")) : null)
            .batchId(val(r, "BATCH_ID") != null ? Refs.batch(val(r, "BATCH_ID")) : null)
            .customerRef(val(r, "CUST_ID") != null || val(r, "XTL_CUST_ID") != null
                ? Refs.customer(val(r, "CUST_ID"), val(r, "XTL_CUST_ID")) : null)
            .savedCardRef(val(r, "PMT_ID") != null || val(r, "PMT_ID_XTL") != null
                ? Refs.savedCard(val(r, "PMT_ID"), val(r, "PMT_ID_XTL")) : null)
            .membershipRef(val(r, "MBSHP_ID") != null || val(r, "MBSHP_ID_XTL") != null
                ? Refs.membership(val(r, "MBSHP_ID"), val(r, "MBSHP_ID_XTL")) : null)
            .lineItemRefs(liRefs)
            .amount(amount)
            .settled(flag(r.get("TRANS_SETTLED")))
            .conversion(conversion)
            .outcome(outcome)
            .serviceClassification(svcClass)
            .avs(avsInfo != null ? new TransactionResult.AvsResult(avsInfo, r.get("AVS_RESPONSE")) : null)
            .cvv(cvvInfo != null ? new TransactionResult.CvvResult(cvvInfo, r.get("CVV_RESPONSE")) : null)
            .card(card(r))
            .nextAction(nextAction(r, status))
            .raw(r)
            .build();
    }

    private static TransactionResult.CardInfo card(Map<String, String> r) {
        boolean has = val(r, "CARD_BRAND_NAME") != null || val(r, "PMT_L4") != null
            || val(r, "CARD_TYPE") != null || val(r, "CARD_BANK") != null
            || val(r, "CARD_COUNTRY") != null;
        if (!has) return null;
        TransactionResult.CardInfo c = new TransactionResult.CardInfo();
        c.brand = r.get("CARD_BRAND_NAME");
        c.detail = r.get("CARD_DETAIL");
        c.type = r.get("CARD_TYPE");
        c.cardClass = r.get("CARD_CLASS");
        c.country = r.get("CARD_COUNTRY");
        c.bank = r.get("CARD_BANK");
        c.prepaid = "1".equals(r.get("CARD_PREPAID"));
        c.balance = r.get("CARD_BALANCE");
        c.last4 = r.get("PMT_L4");
        c.networkTokenUsed = num(r.get("TRANS_NTOKEN_USED"));
        if (val(r, "PMT_AAU_UPDATE_DESC") != null || val(r, "PMT_AAU_UPDATE_DATE") != null) {
            c.accountUpdater = new TransactionResult.AccountUpdater(
                r.get("PMT_AAU_UPDATE_DESC"), r.get("PMT_AAU_UPDATE_DATE"),
                r.get("PMT_AAU_UPDATE_EXPIRY"), r.get("PMT_AAU_UPDATE_L4"));
        }
        return c;
    }

    private static TransactionResult.NextAction nextAction(
        Map<String, String> r, TransactionStatus status) {
        if (status != TransactionStatus.PENDING) return null;
        if (val(r, "P3DS_PROCTRANSID") != null || val(r, "PAREQ") != null
            || val(r, "P3DS_JWT") != null) {
            TransactionResult.NextAction n = new TransactionResult.NextAction("threeDSChallenge");
            n.redirectUrl = r.get("PROC_REDIRECT_URL");
            n.jwt = r.get("P3DS_JWT");
            n.procTransId = r.get("P3DS_PROCTRANSID");
            n.pareq = r.get("PAREQ");
            return n;
        }
        if (val(r, "PROC_BARCODE") != null) {
            TransactionResult.NextAction n = new TransactionResult.NextAction("displayVoucher");
            n.url = r.get("PROC_REDIRECT_URL");
            n.barcode = r.get("PROC_BARCODE");
            return n;
        }
        if (val(r, "PIX_TOKEN") != null) {
            TransactionResult.NextAction n = new TransactionResult.NextAction("displayQr");
            n.url = r.get("PROC_REDIRECT_URL");
            n.token = r.get("PIX_TOKEN");
            return n;
        }
        if (val(r, "PROC_REDIRECT_URL") != null) {
            TransactionResult.NextAction n = new TransactionResult.NextAction("redirect");
            n.url = r.get("PROC_REDIRECT_URL");
            return n;
        }
        return new TransactionResult.NextAction("awaitSettlement");
    }

    /** Net position mirrors BATCH_PKG's sibling-sum keyed on PO_ID (§3.6). */
    public static OrderStatus toOrderStatus(Map<String, String> r, List<TransactionResult> legs) {
        String currency = null;
        for (TransactionResult l : legs) {
            if (l.amount() != null) { currency = l.amount().currency(); break; }
        }
        if (currency == null) {
            currency = val(r, "CURR_CODE_ALPHA") != null ? r.get("CURR_CODE_ALPHA") : "USD";
        }

        BigDecimal auth = BigDecimal.ZERO, cap = BigDecimal.ZERO, ref = BigDecimal.ZERO;
        boolean settled = !legs.isEmpty();
        for (TransactionResult l : legs) {
            String a = l.action() == null ? "" : l.action().toUpperCase();
            boolean isAuth = a.contains("AUTHORIZE") || a.contains("AUTHCAP");
            boolean isCapture = a.contains("CAPTURE") && !a.contains("REVERSECAP");
            boolean isRefund = a.contains("CREDIT") || a.contains("REVERSE");
            boolean approved = l.status() == TransactionStatus.APPROVED;
            if (l.amount() != null && approved) {
                if (isAuth) auth = auth.add(l.amount().amount());
                else if (isCapture) cap = cap.add(l.amount().amount());
                else if (isRefund) ref = ref.add(l.amount().amount());
            }
            if (isAuth && !l.settled()) settled = false;
        }

        return new OrderStatus(
            Refs.order(r.getOrDefault("PO_ID", "unknown")),
            val(r, "XTL_ORDER_ID") != null ? Refs.xtlOrder(val(r, "XTL_ORDER_ID")) : null,
            legs,
            Money.of(auth, currency),
            Money.of(cap, currency),
            Money.of(ref, currency),
            Money.of(cap.subtract(ref), currency),
            Money.of(auth.subtract(cap), currency),
            settled,
            r);
    }
}
