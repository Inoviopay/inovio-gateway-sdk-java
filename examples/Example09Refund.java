import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * refund() — CCCREDIT
 *
 * <p>Returns captured funds to the cardholder. Pass an amount for a partial
 * refund; omit it to refund the full order.
 *
 * <p>Refund legs arrive with a NEGATIVE amount. status() reports
 * {@code refunded} as a positive magnitude, so you rarely think about the sign.
 */
public class Example09Refund {
    public static void main(String[] a) {
        InovioClient c = Harness.client();

        // You can only refund what was captured.
        TransactionResult order = Harness.seedOrder(c, "REFUND", true, "10.00");
        Harness.show("captured order", order.orderRef() == null ? "-" : order.orderRef().poId());

        TransactionResult r = c.refund(order.orderRef(), Money.of("10.00", "USD"));
        Harness.show("status", r.status());
        Harness.show("amount",
            (r.amount() == null ? "-" : r.amount().toWire()) + "   (negative on the wire)");

        // Full refund instead:  c.refund(order.orderRef());
    }
}
