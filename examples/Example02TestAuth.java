import com.inoviopay.gateway.errors.AuthenticationException;
import com.inoviopay.gateway.result.HealthResult;

/**
 * testAuth() — TESTAUTH
 *
 * <p>Verifies your credentials without creating a transaction. Bad credentials
 * raise AuthenticationException (API tier 101), not a decline.
 */
public class Example02TestAuth {
    public static void main(String[] a) {
        try {
            HealthResult h = Harness.client().testAuth();
            Harness.show("ok", h.ok());
            Harness.show("service code",
                h.outcome().service().code() + " \"" + h.outcome().service().advice() + "\"");
        } catch (AuthenticationException e) {
            Harness.show("rejected", e.getMessage());
        }
    }
}
