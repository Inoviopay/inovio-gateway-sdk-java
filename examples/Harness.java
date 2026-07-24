import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.model.PaymentMethods;
import com.inoviopay.gateway.model.RequestParts.Address;
import com.inoviopay.gateway.model.RequestParts.Customer;
import com.inoviopay.gateway.model.RequestParts.Idempotency;
import com.inoviopay.gateway.model.RequestParts.LineItem;
import com.inoviopay.gateway.request.TransactionRequest;
import com.inoviopay.gateway.result.TransactionResult;
import com.inoviopay.gateway.transport.HttpClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared harness for the runnable examples.
 *
 * <p>Every example is real, executed code — not a markdown snippet — so it
 * cannot silently drift from the API.
 *
 * <p>By default they run against a MOCK transport: no credentials, no network,
 * no money moves, safe in CI. Set INOVIO_LIVE=1 (plus credentials) to run the
 * same code against the real gateway.
 */
public final class Harness {

    public static final boolean LIVE = "1".equals(System.getenv("INOVIO_LIVE"));

    private Harness() {}

    static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    /** Canned responses keyed by REQUEST_ACTION, shaped like the real gateway. */
    static final class MockHttp implements HttpClient {
        @Override
        public Response post(String url, String body, Map<String, String> headers, long timeoutMs) {
            if (url.contains("token_service")) {
                return new Response(200, "{\"TOKEN_GUID\":\"F76E1864D6E018BA5D98080167CDF86AD432FEBD\","
                    + "\"TOKEN_IP\":\"10.13.100.134\",\"TOKEN_REQID\":\"4283012\","
                    + "\"CARD_BRAND_NAME\":\"Visa\",\"CARD_TYPE\":\"VISA TRADITIONAL\","
                    + "\"CARD_BANK\":\"CHASE BANK USA\",\"CARD_COUNTRY\":\"USA\","
                    + "\"CARD_ACCOUNT_FUND_SOURCE\":\"Credit\",\"CARD_CLASS\":\"CONSUMER\"}");
            }
            String action = "";
            for (String pair : body.split("&")) {
                if (pair.startsWith("REQUEST_ACTION=")) action = pair.substring(15);
            }
            if ("CCSTATUS".equals(action)) {
                // CCSTATUS answers with a COLUMNS/DATA table, not flat fields.
                return new Response(200, "{\"COLUMNS\":[\"REQUEST_ACTION\",\"TRANS_STATUS_NAME\","
                    + "\"TRANS_VALUE\",\"TRANS_ID\",\"PO_ID\",\"CURR_CODE_ALPHA\"],\"DATA\":["
                    + "[\"CCAUTHORIZE\",\"APPROVED\",100.00,\"T-1\",\"18800001\",\"USD\"],"
                    + "[\"CCCAPTURE\",\"APPROVED\",60.00,\"T-2\",\"18800001\",\"USD\"],"
                    + "[\"CCCREDIT\",\"APPROVED\",-10.00,\"T-3\",\"18800001\",\"USD\"]]}");
            }
            String value = action.contains("REVERSE") || action.contains("CREDIT") ? "-10.00" : "10.00";
            return new Response(200, "{\"REQUEST_ACTION\":\"" + action + "\","
                + "\"TRANS_STATUS_NAME\":\"APPROVED\",\"TRANS_VALUE\":\"" + value + "\","
                + "\"CURR_CODE_ALPHA\":\"USD\",\"PO_ID\":\"18800001\",\"TRANS_ID\":\"2000000001\","
                + "\"PO_LI_ID_1\":\"9000001\",\"PO_LI_ID_2\":\"9000002\",\"API_RESPONSE\":\"0\","
                + "\"SERVICE_RESPONSE\":\"" + ("TESTGW".equals(action) ? "101" : "100") + "\","
                + "\"SERVICE_ADVICE\":\"OK\",\"CARD_BRAND_NAME\":\"Visa\",\"PMT_L4\":\"0647\","
                + "\"AVS_RESPONSE\":\"Y\",\"CVV_RESPONSE\":\"M\"}");
        }
    }

    public static InovioClient client() {
        return client(null, null);
    }

    public static InovioClient client(String siteIdOverride, HttpClient httpOverride) {
        InovioClient.Options o = new InovioClient.Options();
        o.endpoint = env("INOVIO_ENDPOINT", "https://t1api.inoviopay.com/payment/pmt_service.cfm");
        o.siteKey = env("INOVIO_SITE_KEY", "demo-site-key");
        o.timeoutMs = 60_000;
        o.httpClient = httpOverride != null ? httpOverride : (LIVE ? null : new MockHttp());

        String siteId = siteIdOverride != null ? siteIdOverride
            : (LIVE ? System.getenv("INOVIO_SITE_ID") : "100103");
        InovioClient.Credentials creds = LIVE
            ? new InovioClient.Credentials(System.getenv("INOVIO_USER"),
                System.getenv("INOVIO_PASS"), siteId, System.getenv("INOVIO_MERCH_ACCT_ID"))
            : new InovioClient.Credentials("demo@example.invalid", "demo", siteId);
        return new InovioClient(creds, o);
    }

    /**
     * The token service authenticates per SITE with an HMAC key, independent of
     * the gateway's username/password. Normally the same site — but on a shared
     * test rig they can differ.
     */
    public static InovioClient tokenClient() {
        String s = System.getenv("INOVIO_TOKEN_SITE_ID");
        return client(s == null || s.isEmpty() ? null : s, null);
    }

    public static final String PAN = env("INOVIO_TEST_PAN", "4622943123100647");
    public static final String EXPIRY = env("INOVIO_TEST_EXPIRY", "122026");
    public static final String CVV = env("INOVIO_TEST_CVV", "242");
    public static final String PRODUCT_ID = env("INOVIO_TEST_PRODUCT_ID", "111205");

    public static String orderId(String tag) {
        return "EXAMPLE-" + tag + "-" + System.currentTimeMillis();
    }

    public static TransactionRequest request(String tag, String amount) {
        return request(tag, new LineItem(PRODUCT_ID, 1, Money.of(amount, "USD")));
    }

    public static TransactionRequest request(String tag, LineItem... items) {
        TransactionRequest r = new TransactionRequest(
            PaymentMethods.card(PAN, EXPIRY, CVV), items);
        Customer c = new Customer();
        c.firstName = "Ada";
        c.lastName = "Lovelace";
        c.email = "ada@example.invalid";
        // The processor rejects a missing IP with 'remote_ip is missing'.
        c.ip = "203.0.113.10";
        r.customer = c;
        Address a = new Address();
        a.line1 = "123 Main St";
        a.city = "Austin";
        a.state = "TX";
        a.zip = "78701";
        // Country is processor-required despite not being marked so in the spec.
        a.country = "US";
        r.billingAddress = a;
        r.idempotency = new Idempotency(orderId(tag));
        return r;
    }

    /**
     * Create a real order to operate on. Follow-up operations need an order that
     * actually exists, so examples build their own rather than hardcoding an id
     * that resolves only against a mock.
     */
    public static TransactionResult seedOrder(InovioClient c, String tag,
                                              boolean capture, String amount) {
        TransactionResult auth = c.authorize(request(tag, amount));
        if (capture && auth.orderRef() != null) {
            c.capture(auth.orderRef(), Money.of(amount, "USD"));
        }
        return auth;
    }

    public static void show(String label, Object value) {
        System.out.printf("  %-22s %s%n", label, value);
    }
}
