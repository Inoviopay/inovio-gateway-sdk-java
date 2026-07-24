import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * reverseCapture() — CCREVERSECAP
 *
 * <p>VOIDS a capture rather than the original authorization. Reach for this
 * when you captured in error and the batch has not settled yet. After
 * settlement, refund() is the correct operation instead.
 */
public class Example08ReverseCapture {
    public static void main(String[] a) {
        InovioClient c = Harness.client();

        TransactionResult order = Harness.seedOrder(c, "REVCAP", true, "10.00");
        Harness.show("captured order", order.orderRef() == null ? "-" : order.orderRef().poId());

        TransactionResult r = c.reverseCapture(order.orderRef());
        Harness.show("status", r.status());
        Harness.show("amount", r.amount() == null ? "-" : r.amount().toWire());
        Harness.show("when to use", "capture made in error, before batch settlement");
    }
}
