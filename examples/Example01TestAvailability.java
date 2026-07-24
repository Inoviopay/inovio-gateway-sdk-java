import com.inoviopay.gateway.result.HealthResult;

/**
 * testAvailability() — TESTGW
 *
 * <p>Health check for the gateway itself. No credentials are validated and no
 * transaction is created, so it is safe to poll.
 */
public class Example01TestAvailability {
    public static void main(String[] a) {
        HealthResult h = Harness.client().testAvailability();
        Harness.show("ok", h.ok());
        Harness.show("service code",
            h.outcome().service().code() + " \"" + h.outcome().service().advice() + "\"");
    }
}
