import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * reverse() — CCREVERSE
 *
 * <p>VOIDS an authorization, releasing the hold. This is not a refund: nothing
 * was captured, so nothing is returned. Use it when an order is cancelled
 * before shipping. To void a CAPTURE instead, use reverseCapture().
 */
public class Example07Reverse {
    public static void main(String[] a) {
        InovioClient c = Harness.client();

        TransactionResult auth = c.authorize(Harness.request("REV", "10.00"));
        Harness.show("authorized", auth.status() + " order="
            + (auth.orderRef() == null ? "-" : auth.orderRef().poId()));

        TransactionResult voided = c.reverse(auth.orderRef());
        Harness.show("reversed", voided.status());
        // Void legs come back with a negative amount.
        Harness.show("amount", voided.amount() == null ? "-" : voided.amount().toWire());
        Harness.show("effect", "authorization released — order nets to zero");
    }
}
