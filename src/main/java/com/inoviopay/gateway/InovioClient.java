package com.inoviopay.gateway;

import com.inoviopay.gateway.enums.ApiResponseCodes;
import com.inoviopay.gateway.enums.RequestAction;
import com.inoviopay.gateway.enums.SpecVersion;
import com.inoviopay.gateway.enums.TransactionStatus;
import com.inoviopay.gateway.errors.AuthenticationException;
import com.inoviopay.gateway.errors.ConfigurationException;
import com.inoviopay.gateway.errors.RateLimitException;
import com.inoviopay.gateway.errors.ValidationException;
import com.inoviopay.gateway.model.Card;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.model.PaymentMethods;
import com.inoviopay.gateway.model.Token;
import com.inoviopay.gateway.refs.Refs;
import com.inoviopay.gateway.request.OrderUpdate;
import com.inoviopay.gateway.request.RequestBuilder;
import com.inoviopay.gateway.request.TransactionRequest;
import com.inoviopay.gateway.result.HealthResult;
import com.inoviopay.gateway.result.OrderStatus;
import com.inoviopay.gateway.result.ResultMapper;
import com.inoviopay.gateway.result.TransactionResult;
import com.inoviopay.gateway.transport.HttpClient;
import com.inoviopay.gateway.transport.JdkHttpClient;
import com.inoviopay.gateway.transport.Transport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The v1 card-core surface (object model §3.1).
 *
 * <p>Partners call {@code client.sale()}, never {@code REQUEST_ACTION=CCAUTHCAP}.
 */
public final class InovioClient {

    private static final long DEFAULT_TIMEOUT_MS = 120_000L;
    private static final Pattern INDEXED = Pattern.compile("(.*?)_(\\d+)");
    private static final Pattern LEG_FIELD =
        Pattern.compile("^(TRANS_|REQUEST_ACTION|SERVICE_|PROCESSOR_|API_|AVS_|CVV_|PO_ID).*");

    /** Gateway credentials. */
    public static final class Credentials {
        final String reqUsername;
        final String reqPassword;
        final String siteId;
        final String merchAcctId;

        public Credentials(String reqUsername, String reqPassword, String siteId) {
            this(reqUsername, reqPassword, siteId, null);
        }

        public Credentials(String reqUsername, String reqPassword, String siteId,
                           String merchAcctId) {
            if (reqUsername == null || reqUsername.isEmpty()
                || reqPassword == null || reqPassword.isEmpty()
                || siteId == null || siteId.isEmpty()) {
                throw new ValidationException(
                    "credentials require reqUsername, reqPassword and siteId");
            }
            this.reqUsername = reqUsername;
            this.reqPassword = reqPassword;
            this.siteId = siteId;
            this.merchAcctId = merchAcctId;
        }
    }

    /** Optional client configuration. */
    public static final class Options {
        public Transport.Environment environment = Transport.Environment.SANDBOX;
        /** Overrides the environment endpoint entirely (local stack, proxy). */
        public String endpoint;
        public String tokenEndpoint;
        public String apiVersion = SpecVersion.API_VERSION;
        public long timeoutMs = DEFAULT_TIMEOUT_MS;
        public HttpClient httpClient;
        /**
         * Per-site HMAC secret for the token service (spec §4.8). Issued by
         * Inovio support; distinct from the gateway password. tokenize() only.
         */
        public String siteKey;
    }

    private final Credentials creds;
    private final String endpoint;
    private final String tokenEndpoint;
    private final String apiVersion;
    private final long timeoutMs;
    private final HttpClient http;
    private final String siteKey;

    public InovioClient(Credentials creds) {
        this(creds, new Options());
    }

    public InovioClient(Credentials creds, Options options) {
        this.creds = creds;
        Options o = options == null ? new Options() : options;
        this.endpoint = o.endpoint != null ? o.endpoint : o.environment.endpoint();
        this.tokenEndpoint = o.tokenEndpoint != null
            ? o.tokenEndpoint
            : this.endpoint.replaceAll("pmt_service\\.cfm$", "token_service.cfm");
        this.apiVersion = o.apiVersion;
        this.timeoutMs = o.timeoutMs;
        this.http = o.httpClient != null ? o.httpClient : new JdkHttpClient();
        this.siteKey = o.siteKey;
    }

    // ------------------------------------------------------------------

    private Map<String, String> authParams(String action) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("REQ_USERNAME", creds.reqUsername);
        p.put("REQ_PASSWORD", creds.reqPassword);
        p.put("SITE_ID", creds.siteId);
        p.put("REQUEST_ACTION", action);
        p.put("REQUEST_API_VERSION", apiVersion);
        p.put("REQUEST_RESPONSE_FORMAT", "JSON");
        if (creds.merchAcctId != null) p.put("MERCH_ACCT_ID", creds.merchAcctId);
        return p;
    }

    /**
     * Raise for API-tier failures only.
     *
     * <p>A DECLINE IS NOT AN ERROR — it returns normally as a
     * {@code TransactionResult} with status {@code DECLINED} (Q1).
     */
    private void raiseIfApiError(Map<String, String> r) {
        String code = r.get("API_RESPONSE");
        if (code == null || code.isEmpty()) return;
        int c;
        try {
            c = Integer.parseInt(code.trim());
        } catch (NumberFormatException e) {
            return;
        }
        if (c == 0) return;
        ApiResponseCodes.Info info = ApiResponseCodes.get(c);
        if (info == null) return;
        String msg = info.description()
            + (info.recommendation() == null || info.recommendation().isEmpty()
                ? "" : " — " + info.recommendation());
        switch (info.mapsToException()) {
            case "RateLimitException":
                throw new RateLimitException(msg, r);
            case "AuthenticationException":
                throw new AuthenticationException(msg, c, r);
            case "ValidationException":
                throw new ValidationException(msg, c, r.get("REF_FIELD"), r);
            case "ConfigurationException":
                throw new ConfigurationException(msg, c, r);
            default:
                // unknown mapping — let the result carry the tier detail
        }
    }

    private Map<String, String> call(String action, Map<String, String> params,
                                     String idempotencyKey) {
        Map<String, String> merged = authParams(action);
        merged.putAll(params);
        Map<String, String> raw =
            Transport.send(endpoint, http, timeoutMs, merged, idempotencyKey);
        raiseIfApiError(raw);
        return raw;
    }

    private TransactionResult transact(RequestAction action, TransactionRequest req) {
        Map<String, String> params = RequestBuilder.build(req);
        String key = req.idempotency == null ? null : req.idempotency.xtlOrderId;
        return ResultMapper.toTransactionResult(call(action.wire(), params, key));
    }

    private Map<String, String> amountParams(String refKey, String refValue, Money amount) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put(refKey, refValue);
        if (amount != null) {
            p.put("LI_VALUE_1", amount.toWire());
            p.put("LI_COUNT_1", "1");
            p.put("REQUEST_CURRENCY", amount.currency());
        }
        return p;
    }

    // ---------------------------- operations --------------------------

    /** CCAUTHCAP — authorize and capture in one step. */
    public TransactionResult sale(TransactionRequest req) {
        return transact(RequestAction.CCAUTHCAP, req);
    }

    /** CCAUTHORIZE — authorization only; capture later. */
    public TransactionResult authorize(TransactionRequest req) {
        return transact(RequestAction.CCAUTHORIZE, req);
    }

    /** CCCAPTURE — capture a previous authorization. Partial-capable. */
    public TransactionResult capture(Refs.OrderRef order, Money amount) {
        return ResultMapper.toTransactionResult(call(RequestAction.CCCAPTURE.wire(),
            amountParams("REQUEST_REF_PO_ID", order.poId(), amount), null));
    }

    public TransactionResult capture(Refs.OrderRef order) {
        return capture(order, null);
    }

    /**
     * CCCAPTURE against a single line item.
     *
     * <p>Per spec §5.5.6 the gateway requires the PARENT ORDER and an amount
     * alongside the line-item id — sending {@code REQUEST_REF_PO_LI_ID} alone
     * is rejected with API 113 "Invalid Data". {@code LineItemRef} does not
     * carry its order, so both must be passed. Verified against live T1.
     *
     * @param order the order the line item belongs to (gateway-required)
     * @param item the line item, from {@code result.lineItemRefs()}
     * @param amount required — a line-item capture without LI_VALUE_1 is rejected
     */
    public TransactionResult captureLineItem(Refs.OrderRef order, Refs.LineItemRef item,
                                             Money amount) {
        if (amount == null) {
            throw new ValidationException(
                "captureLineItem requires an amount — the gateway rejects a line-item "
                    + "capture without LI_VALUE_1 (spec §5.5.6)", "LI_VALUE_1");
        }
        Map<String, String> p = new LinkedHashMap<>();
        p.put("REQUEST_REF_PO_ID", order.poId());
        p.put("REQUEST_REF_PO_LI_ID", item.poLiId());
        p.put("LI_VALUE_1", amount.toWire());
        p.put("LI_COUNT_1", "1");
        p.put("REQUEST_CURRENCY", amount.currency());
        return ResultMapper.toTransactionResult(
            call(RequestAction.CCCAPTURE.wire(), p, null));
    }

    /** CCREVERSE — void the original authorization. */
    public TransactionResult reverse(Refs.OrderRef order) {
        return ResultMapper.toTransactionResult(call(RequestAction.CCREVERSE.wire(),
            amountParams("REQUEST_REF_PO_ID", order.poId(), null), null));
    }

    /** CCREVERSECAP — void a CCCAPTURE (not the original auth). */
    public TransactionResult reverseCapture(Refs.OrderRef order) {
        return ResultMapper.toTransactionResult(call(RequestAction.CCREVERSECAP.wire(),
            amountParams("REQUEST_REF_PO_ID", order.poId(), null), null));
    }

    /** CCCREDIT — refund against an existing order. Partial-capable. */
    public TransactionResult refund(Refs.OrderRef order, Money amount) {
        return ResultMapper.toTransactionResult(call(RequestAction.CCCREDIT.wire(),
            amountParams("REQUEST_REF_PO_ID", order.poId(), amount), null));
    }

    public TransactionResult refund(Refs.OrderRef order) {
        return refund(order, null);
    }

    /** CCCREDIT + FORCE_CREDIT — a credit with no referenced original. */
    public TransactionResult forceCredit(TransactionRequest req) {
        req.forceCredit = true;
        return transact(RequestAction.CCCREDIT, req);
    }

    /**
     * CCSTATUS — the reconciliation primitive AND the unknown-state recovery path.
     *
     * <p>Returns order-level net position derived from every leg sharing the
     * PO_ID. For any order with more than one leg this is the only correct
     * source of net figures.
     */
    public OrderStatus status(Refs.OrderRef order) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("REQUEST_REF_PO_ID", order.poId());
        return buildStatus(call(RequestAction.CCSTATUS.wire(), p, null));
    }

    public OrderStatus status(Refs.XtlOrderId xtlOrder) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("REQUEST_REF_PO_ID_XTL", xtlOrder.value());
        return buildStatus(call(RequestAction.CCSTATUS.wire(), p, null));
    }

    private OrderStatus buildStatus(Map<String, String> raw) {
        List<Map<String, String>> legMaps = extractLegs(raw);
        List<TransactionResult> legs = new ArrayList<>();
        if (legMaps.isEmpty()) {
            legs.add(ResultMapper.toTransactionResult(raw));
        } else {
            for (Map<String, String> leg : legMaps) {
                legs.add(ResultMapper.toTransactionResult(leg));
            }
        }
        return ResultMapper.toOrderStatus(raw, legs);
    }

    /** CCTRANSUPDATE — attach receipts to an existing order (Appendix G/J). */
    public TransactionResult updateOrder(Refs.OrderRef order, OrderUpdate update) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("REQUEST_REF_PO_ID", order.poId());
        if (update.receipt != null) p.put("RECEIPT", update.receipt);
        if (update.metadata != null) {
            for (Map.Entry<String, String> e : update.metadata.udf.entrySet()) {
                String k = e.getKey();
                p.put("XTL_UDF" + (k.length() < 2 ? "0" + k : k), e.getValue());
            }
        }
        return ResultMapper.toTransactionResult(
            call(RequestAction.CCTRANSUPDATE.wire(), p, null));
    }

    /**
     * Ephemeral tokenization (spec §4.8) — exchange a PAN for a single-use
     * {@code TOKEN_GUID} usable in place of {@code PMT_NUMB}.
     *
     * <p>Requires {@code siteKey} on {@link Options} — the per-site HMAC secret
     * issued by Inovio support. It is NOT the gateway password; without it the
     * token service answers error 121.
     *
     * <p>NOTE: this is a server-side call — the PAN passes through your
     * infrastructure, so you remain in your server's data flow. The low-scope path is the
     * browser Hosted Fields client.
     */
    public Tokenize.Result tokenize(Card card) {
        return tokenize(card, null);
    }

    public Tokenize.Result tokenize(Card card, String uniqueId) {
        if (siteKey == null || siteKey.isEmpty()) {
            throw new ValidationException(
                "tokenize requires Options.siteKey — the per-site HMAC secret from "
                    + "Inovio support (not your gateway password).");
        }
        return Tokenize.tokenize(card, tokenEndpoint, http, timeoutMs,
            creds.siteId, siteKey, apiVersion, uniqueId);
    }

    /** TESTAUTH — verify credentials. */
    public HealthResult testAuth() {
        return toHealth(call(RequestAction.TESTAUTH.wire(), new HashMap<>(), null));
    }

    /** TESTGW — verify gateway availability. */
    public HealthResult testAvailability() {
        return toHealth(call(RequestAction.TESTGW.wire(), new HashMap<>(), null));
    }

    private HealthResult toHealth(Map<String, String> raw) {
        TransactionResult res = ResultMapper.toTransactionResult(raw);
        String svc = raw.get("SERVICE_RESPONSE");
        boolean ok = res.status() == TransactionStatus.APPROVED
            || "100".equals(svc) || "101".equals(svc);
        return new HealthResult(ok, raw.getOrDefault("REQUEST_ACTION", ""),
            res.outcome(), res.raw());
    }

    /**
     * CCSTATUS does not answer with flat fields like every other action — it
     * returns a tabular payload:
     *
     * <pre>
     * { "COLUMNS": ["REQUEST_ACTION","TRANS_STATUS_NAME",...],
     *   "DATA":    [ ["CCAUTHORIZE","APPROVED",...], ["CCCAPTURE",...] ] }
     * </pre>
     *
     * <p>One DATA row per leg against the order. Verified against the live T1
     * gateway; this shape is not described in the v4.14 response-fields section.
     */
    static List<Map<String, String>> extractLegs(Map<String, String> raw) {
        String tabular = raw.get("__TABULAR__");
        if (tabular == null) return new ArrayList<>();

        List<String> columns = jsonStringArray(tabular, "COLUMNS");
        List<List<String>> rows = jsonRowArray(tabular, "DATA");
        List<Map<String, String>> legs = new ArrayList<>();
        for (List<String> row : rows) {
            Map<String, String> leg = new LinkedHashMap<>();
            for (int i = 0; i < columns.size() && i < row.size(); i++) {
                String v = row.get(i);
                if (v == null || v.isEmpty()) continue;
                // Duplicate column names appear (TRANS_ID twice); first wins.
                leg.putIfAbsent(columns.get(i).toUpperCase(), v);
            }
            legs.add(leg);
        }
        return legs;
    }

    /** Read a flat ["a","b"] array out of the raw JSON by key. */
    private static List<String> jsonStringArray(String json, String key) {
        List<String> out = new ArrayList<>();
        int k = json.indexOf('"' + key + '"');
        if (k < 0) return out;
        int start = json.indexOf('[', k);
        int end = json.indexOf(']', start);
        if (start < 0 || end < 0) return out;
        for (String part : json.substring(start + 1, end).split(",")) {
            out.add(part.trim().replaceAll("^\"|\"$", ""));
        }
        return out;
    }

    /** Read a [[...],[...]] array of rows out of the raw JSON by key. */
    private static List<List<String>> jsonRowArray(String json, String key) {
        List<List<String>> rows = new ArrayList<>();
        int k = json.indexOf('"' + key + '"');
        if (k < 0) return rows;
        int i = json.indexOf('[', k) + 1;
        while (i < json.length()) {
            int rowStart = json.indexOf('[', i);
            if (rowStart < 0) break;
            int rowEnd = json.indexOf(']', rowStart);
            if (rowEnd < 0) break;
            List<String> row = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            boolean inStr = false;
            for (int j = rowStart + 1; j < rowEnd; j++) {
                char c = json.charAt(j);
                if (c == '"') { inStr = !inStr; continue; }
                if (c == ',' && !inStr) { row.add(cur.toString().trim()); cur.setLength(0); continue; }
                cur.append(c);
            }
            row.add(cur.toString().trim());
            rows.add(row);
            i = rowEnd + 1;
            if (json.indexOf('[', i) < 0 || json.indexOf(']', i) < json.indexOf('[', i)) break;
        }
        return rows;
    }
}
