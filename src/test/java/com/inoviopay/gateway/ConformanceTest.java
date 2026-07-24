package com.inoviopay.gateway;

import com.inoviopay.gateway.enums.AvsCodes;
import com.inoviopay.gateway.enums.ServiceResponseCodes;
import com.inoviopay.gateway.enums.SpecVersion;
import com.inoviopay.gateway.enums.TransactionStatus;
import com.inoviopay.gateway.errors.GatewayTimeoutException;
import com.inoviopay.gateway.errors.ValidationException;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.model.PaymentMethods;
import com.inoviopay.gateway.model.RequestParts.LineItem;
import com.inoviopay.gateway.refs.Refs;
import com.inoviopay.gateway.request.TransactionRequest;
import com.inoviopay.gateway.result.OrderStatus;
import com.inoviopay.gateway.result.TransactionResult;
import com.inoviopay.gateway.transport.HttpClient;
import com.inoviopay.gateway.transport.Transport;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Conformance against the shared corpus in
 * {@code ../../spec/conformance-fixtures.json}.
 *
 * <p>Java has no zero-dependency JSON reader in the test scope, so rather than
 * add one the fixtures are transcribed here as literal cases. Each test name
 * matches a fixture name so drift is visible; the assertions are identical to
 * the ones the Node and Python suites make.
 */
class ConformanceTest {

    /** Captures outgoing params and replays a canned response. */
    static final class MockHttp implements HttpClient {
        private final Map<String, String> response;
        private final boolean timeout;
        Map<String, String> lastParams = new LinkedHashMap<>();

        MockHttp(Map<String, String> response) { this(response, false); }

        MockHttp(Map<String, String> response, boolean timeout) {
            this.response = response;
            this.timeout = timeout;
        }

        @Override
        public Response post(String url, String body, Map<String, String> headers, long timeoutMs) {
            lastParams = Transport.normalizeResponse(body);
            if (timeout) throw new TimeoutSignal("simulated", null);
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> e : response.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
                first = false;
            }
            return new Response(200, sb.append('}').toString());
        }
    }

    private static final InovioClient.Credentials CREDS =
        new InovioClient.Credentials("u", "p", "1");

    private static InovioClient client(HttpClient http) {
        InovioClient.Options o = new InovioClient.Options();
        o.httpClient = http;
        o.endpoint = "https://gateway.invalid/payment/pmt_service.cfm";
        o.timeoutMs = 50;
        return new InovioClient(CREDS, o);
    }

    private static Map<String, String> resp(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static TransactionRequest req() {
        return new TransactionRequest(
            PaymentMethods.card("4111111111111111", "122030", "123"),
            new LineItem("SKU-1", 1, Money.of("10.00", "USD")));
    }

    // ---------------------------------------------------------------- approve

    @Test
    void approve_basicSale() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "10.00", "CURR_CODE_ALPHA", "USD", "TRANS_ID", "T-1001",
            "PO_ID", "PO-1001", "REQ_ID", "R-1001", "API_RESPONSE", "0",
            "SERVICE_RESPONSE", "100", "PROCESSOR_RESPONSE", "00",
            "TRANS_SETTLED", "0", "CARD_BRAND_NAME", "VISA", "PMT_L4", "1111"));

        TransactionResult r = client(http).sale(req());

        assertEquals(TransactionStatus.APPROVED, r.status());
        assertEquals(false, r.settling());
        assertEquals(false, r.settled());
        assertEquals("PO-1001", r.orderRef().poId());
        assertEquals("T-1001", r.transactionId().value());
        assertEquals("10.00", r.amount().toWire());
        assertEquals("USD", r.amount().currency());
        assertEquals(Integer.valueOf(100), r.outcome().service().code());
        assertTrue(r.serviceClassification().approval());
        assertEquals(false, r.serviceClassification().terminal());
        assertEquals("VISA", r.card().brand);
        assertEquals("1111", r.card().last4);
        assertNull(r.conversion());
        assertNull(r.nextAction());
    }

    // ---------------------------------------------------------------- decline

    @Test
    void decline_serviceTierIsNotAnException() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "DECLINED",
            "TRANS_VALUE", "25.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1002",
            "API_RESPONSE", "0", "SERVICE_RESPONSE", "600",
            "PROCESSOR_RESPONSE", "05", "PROCESSOR_ADVICE", "Do not honor"));

        TransactionResult r = client(http).sale(req());   // must NOT throw

        assertEquals(TransactionStatus.DECLINED, r.status());
        assertEquals(false, r.settling());
        assertEquals(Integer.valueOf(600), r.outcome().service().code());
        assertEquals("Do not honor", r.outcome().processor().advice());
        assertTrue(r.serviceClassification().terminal());
        assertEquals(false, r.serviceClassification().retryable());
    }

    @Test
    void decline_retryable640() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "DECLINED",
            "TRANS_VALUE", "5.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1003",
            "SERVICE_RESPONSE", "640", "API_RESPONSE", "0"));

        TransactionResult r = client(http).sale(req());

        assertEquals(TransactionStatus.DECLINED, r.status());
        assertTrue(r.serviceClassification().retryable());
        assertEquals(false, r.serviceClassification().terminal());
    }

    @Test
    void decline_stopRecurring219() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "DECLINED",
            "TRANS_VALUE", "9.99", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1004",
            "SERVICE_RESPONSE", "219", "API_RESPONSE", "0"));

        TransactionRequest r = new TransactionRequest(
            PaymentMethods.savedCardByPmtId("PM-9"),
            new LineItem("SKU-1", 1, Money.of("9.99", "USD")));

        assertTrue(client(http).sale(r).serviceClassification().stopRecurring());
    }

    // -------------------------------------------------------------- AVS / CVV

    @Test
    void avs_partialStreetMatch() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "1.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1005",
            "SERVICE_RESPONSE", "100", "API_RESPONSE", "0",
            "AVS_RESPONSE", "A", "CVV_RESPONSE", "M"));

        TransactionResult r = client(http).sale(req());

        assertEquals("A", r.avs().code());
        assertEquals("partial", r.avs().classification());
        assertEquals("M", r.cvv().code());
        assertEquals("match", r.cvv().classification());
    }

    @Test
    void avs_negativeNoMatch() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "1.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1006",
            "SERVICE_RESPONSE", "100", "API_RESPONSE", "0",
            "AVS_RESPONSE", "N", "CVV_RESPONSE", "N"));

        TransactionResult r = client(http).sale(req());

        assertEquals("negative", r.avs().classification());
        assertEquals("no_match", r.cvv().classification());
    }

    // ------------------------------------------------------------------- 3DS

    @Test
    void threeDs_challengePending() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "PENDING",
            "TRANS_VALUE", "50.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1007",
            "API_RESPONSE", "0", "SERVICE_RESPONSE", "100",
            "PROC_REDIRECT_URL", "https://acs.example.invalid/challenge",
            "P3DS_PROCTRANSID", "3DS-77", "PAREQ", "eJxVUk1v"));

        TransactionResult r = client(http).sale(req());

        assertEquals(TransactionStatus.PENDING, r.status());
        assertTrue(r.settling());
        assertEquals("threeDSChallenge", r.nextAction().kind());
        assertEquals("3DS-77", r.nextAction().procTransId);
        assertEquals("https://acs.example.invalid/challenge", r.nextAction().redirectUrl);
    }

    @Test
    void threeDs_frictionlessApproved() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "50.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1008",
            "API_RESPONSE", "0", "SERVICE_RESPONSE", "100"));

        TransactionResult r = client(http).sale(req());

        assertEquals(TransactionStatus.APPROVED, r.status());
        assertEquals(false, r.settling());
        assertNull(r.nextAction());
    }

    // ---------------------------------------------------------- multicurrency

    @Test
    void multicurrency_conversionPresent() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "100.00", "CURR_CODE_ALPHA", "EUR",
            "TRANS_VALUE_SETTLED", "108.50", "CURR_CODE_ALPHA_SETTLED", "USD",
            "TRANS_EXCH_RATE", "1.085", "PO_ID", "PO-1009",
            "API_RESPONSE", "0", "SERVICE_RESPONSE", "100"));

        TransactionRequest r = new TransactionRequest(
            PaymentMethods.card("4111111111111111", "122030"),
            new LineItem("SKU-1", 1, Money.of("100.00", "EUR")));
        TransactionResult res = client(http).sale(r);

        assertEquals("108.50", res.conversion().amount().toWire());
        assertEquals("USD", res.conversion().amount().currency());
        assertEquals("1.085", res.conversion().exchangeRate());
    }

    @Test
    void multicurrency_noConversionDomestic() {
        // settled fields echo the auth amount with no rate -> conversion MUST be
        // absent, otherwise it is always present and means nothing
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "20.00", "CURR_CODE_ALPHA", "USD",
            "TRANS_VALUE_SETTLED", "20.00", "CURR_CODE_ALPHA_SETTLED", "USD",
            "PO_ID", "PO-1010", "API_RESPONSE", "0", "SERVICE_RESPONSE", "100"));

        assertNull(client(http).sale(req()).conversion());
    }

    // ------------------------------------------------------- partial / idempotency

    @Test
    void partialAuth_approvedLesserAmount() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHORIZE", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "40.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1011",
            "API_RESPONSE", "0", "SERVICE_RESPONSE", "100"));

        TransactionRequest r = new TransactionRequest(
            PaymentMethods.card("4111111111111111", "122030"),
            new LineItem("SKU-1", 1, Money.of("100.00", "USD")));
        r.partialAuth = new com.inoviopay.gateway.model.RequestParts.PartialAuth(true);
        r.partialAuth.minimumAmount = Money.of("25.00", "USD");

        TransactionResult res = client(http).authorize(r);

        assertEquals("1", http.lastParams.get("PARTIAL_AUTH"));
        assertEquals("25.00", http.lastParams.get("PARTIAL_AUTH_MIN"));
        assertEquals("40.00", res.amount().toWire());
    }

    @Test
    void idempotency_defaultsToReturnOriginal() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "7.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-1012",
            "XTL_ORDER_ID", "ORD-555", "API_RESPONSE", "0", "SERVICE_RESPONSE", "100"));

        TransactionResult r = client(http).sale(req().idempotency("ORD-555"));

        assertEquals("ORD-555", http.lastParams.get("XTL_ORDER_ID"));
        assertEquals("2", http.lastParams.get("UNIQUE_XTL_ORDER_ID"));
        assertEquals("ORD-555", r.xtlOrderRef().value());
    }

    // ------------------------------------------------------------- api errors

    @Test
    void apiError_authenticationThrows() {
        MockHttp http = new MockHttp(resp(
            "API_RESPONSE", "101", "API_ADVICE", "Invalid login information"));

        assertThrows(com.inoviopay.gateway.errors.AuthenticationException.class,
            () -> client(http).sale(req()));
    }

    @Test
    void apiError_validationCarriesRefField() {
        MockHttp http = new MockHttp(resp(
            "API_RESPONSE", "110", "API_ADVICE", "Required field",
            "REF_FIELD", "CUST_EMAIL"));

        ValidationException e = assertThrows(ValidationException.class,
            () -> client(http).sale(req()));
        assertEquals("CUST_EMAIL", e.refField());
    }

    @Test
    void timeout_unknownStateCarriesKey() {
        MockHttp http = new MockHttp(resp(), true);

        GatewayTimeoutException e = assertThrows(GatewayTimeoutException.class,
            () -> client(http).sale(req().idempotency("ORD-TIMEOUT-1")));
        assertEquals("ORD-TIMEOUT-1", e.xtlOrderId());
        assertTrue(e.recoveryHint().contains("UNKNOWN"));
    }

    // ------------------------------------------------------------------ status

    @Test
    void status_netPositionMultiLeg() {
        // auth 100, capture 60, refund 10 -> net 50, outstanding 40
        MockHttp http = new MockHttp(resp(
            "PO_ID", "PO-2000", "CURR_CODE_ALPHA", "USD", "API_RESPONSE", "0",
            "REQUEST_ACTION_1", "CCAUTHORIZE", "TRANS_STATUS_NAME_1", "APPROVED",
            "TRANS_VALUE_1", "100.00", "TRANS_ID_1", "T-1",
            "REQUEST_ACTION_2", "CCCAPTURE", "TRANS_STATUS_NAME_2", "APPROVED",
            "TRANS_VALUE_2", "60.00", "TRANS_ID_2", "T-2",
            "REQUEST_ACTION_3", "CCCREDIT", "TRANS_STATUS_NAME_3", "APPROVED",
            "TRANS_VALUE_3", "10.00", "TRANS_ID_3", "T-3"));

        OrderStatus s = client(http).status(Refs.order("PO-2000"));

        assertEquals(3, s.transactions().size());
        assertEquals("100.00", s.authorized().toWire());
        assertEquals("60.00", s.captured().toWire());
        assertEquals("10.00", s.refunded().toWire());
        assertEquals("50.00", s.net().toWire());
        assertEquals("40.00", s.outstanding().toWire());
    }

    // ------------------------------------------------------------------- misc

    @Test
    void money_rejectsFloat() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(1.25, "USD"));
    }

    @Test
    void unknownStatus_doesNotReadAsApproved() {
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "WAT",
            "PO_ID", "PO-1013", "API_RESPONSE", "0", "SERVICE_RESPONSE", "100"));

        assertNotEquals(TransactionStatus.APPROVED, client(http).sale(req()).status());
    }

    // ------------------------------ empty-string fields (live-gateway bug)

    @Test
    void emptyStringFields_treatedAsAbsent() {
        // The gateway returns inapplicable fields as EMPTY STRINGS rather than
        // omitting them — verified against the live T1 gateway on TESTGW and
        // TESTAUTH. A plain null check treats those as present and hands "" to
        // a reference constructor, which throws. The mocked fixtures never
        // caught it because they omit the keys entirely.
        MockHttp http = new MockHttp(resp(
            "REQUEST_ACTION", "CCAUTHCAP", "TRANS_STATUS_NAME", "APPROVED",
            "TRANS_VALUE", "1.00", "CURR_CODE_ALPHA", "USD", "PO_ID", "PO-EMPTY",
            "TRANS_ID", "", "CUST_ID", "", "XTL_CUST_ID", "", "PMT_ID", "",
            "BATCH_ID", "", "MERCH_ACCT_ID", "", "CARD_BRAND_NAME", "",
            "PMT_L4", "", "AVS_RESPONSE", "",
            "API_RESPONSE", "0", "SERVICE_RESPONSE", "100"));

        TransactionResult r = client(http).sale(req());   // must not throw

        assertEquals(TransactionStatus.APPROVED, r.status());
        assertEquals("PO-EMPTY", r.orderRef().poId());
        assertNull(r.transactionId(), "empty TRANS_ID must map to null");
        assertNull(r.customerRef(), "empty CUST_ID must map to null");
        assertNull(r.savedCardRef(), "empty PMT_ID must map to null");
        assertNull(r.batchId(), "empty BATCH_ID must map to null");
        assertNull(r.card(), "all-empty card fields must map to null");
        assertNull(r.avs(), "empty AVS_RESPONSE must map to null");
    }

    // ------------------------------------------ generated enums / cross-language

    @Test
    void generatedEnumsMatchSpec() {
        assertEquals("4.14", SpecVersion.API_VERSION);
        assertEquals(5, TransactionStatus.values().length);
        assertTrue(ServiceResponseCodes.get(640).retryable());
        assertTrue(ServiceResponseCodes.get(219).stopRecurring());
        assertTrue(ServiceResponseCodes.get(100).approval());
        // AVS 'A' is partial (street matches, postal does not) — not positive
        assertEquals("partial", AvsCodes.get("A").classification());
        assertEquals("negative", AvsCodes.get("N").classification());
        assertEquals("positive", AvsCodes.get("X").classification());
    }
}
