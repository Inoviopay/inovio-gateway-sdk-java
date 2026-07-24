import com.inoviopay.gateway.errors.GatewayTimeoutException;
import com.inoviopay.gateway.transport.HttpClient;

import java.util.Map;

/**
 * Timeout recovery — the pattern that prevents double charges.
 *
 * <p>A timeout does NOT mean the transaction failed. It means the state is
 * UNKNOWN: the gateway may have approved it and lost the response. Retrying
 * blindly can charge the customer twice.
 *
 * <p>Two mechanisms work together:
 *
 * <ol>
 *   <li>IDEMPOTENCY. Setting an order id defaults to RETURN_ORIGINAL, so a
 *       repeat returns the original result rather than charging twice.
 *   <li>status(). GatewayTimeoutException carries your order id, so you can ask
 *       the gateway what actually happened.
 * </ol>
 */
public class Example14TimeoutRecovery {

    /** A transport that always times out, so the example is deterministic. */
    static final class AlwaysTimesOut implements HttpClient {
        @Override
        public Response post(String url, String body, Map<String, String> h, long t) {
            throw new TimeoutSignal("simulated", null);
        }
    }

    public static void main(String[] a) {
        try {
            Harness.client(null, new AlwaysTimesOut())
                .sale(Harness.request("TIMEOUT", "10.00"));
        } catch (GatewayTimeoutException e) {
            Harness.show("caught", e.getClass().getSimpleName());
            Harness.show("order id", e.xtlOrderId() == null
                ? "(none — cannot resolve)" : e.xtlOrderId());
            Harness.show("guidance", e.recoveryHint());

            // Resolve the true state instead of guessing:
            //   OrderStatus actual = client().status(Refs.xtlOrder(e.xtlOrderId()));
            Harness.show("do NOT", "retry blindly — that risks a double charge");
        }
    }
}
