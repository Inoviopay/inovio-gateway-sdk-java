import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * capture() — CCCAPTURE
 *
 * <p>Takes funds against a prior authorize(). Pass an amount to capture less
 * than was authorized; omit it to capture the full amount.
 *
 * <p>Captures are separate transactions sharing the order, so an order may have
 * several. Use status() for the net position.
 */
public class Example05Capture {
    public static void main(String[] a) {
        InovioClient c = Harness.client();

        TransactionResult auth = c.authorize(Harness.request("CAP", "10.00"));
        Harness.show("authorized", auth.status() + " order="
            + (auth.orderRef() == null ? "-" : auth.orderRef().poId()));

        TransactionResult cap = c.capture(auth.orderRef(), Money.of("10.00", "USD"));
        Harness.show("captured", cap.status());
        Harness.show("settled", cap.settled() + "  (batch flips this later — not a failure)");

        // Or capture the full authorized amount:  c.capture(auth.orderRef());
    }
}
