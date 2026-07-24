import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.result.OrderStatus;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * status() — CCSTATUS
 *
 * <p>Two distinct jobs:
 *
 * <ol>
 *   <li>RECONCILIATION. Partial captures, refunds and voids are separate
 *       transactions sharing one order — so the net position is an order-level
 *       question. One TransactionResult cannot answer "what did this order
 *       actually settle for". This can.
 *   <li>TIMEOUT RECOVERY. After a timeout the state is unknown; status()
 *       resolves it. See Example14TimeoutRecovery.
 * </ol>
 */
public class Example11Status {
    public static void main(String[] a) {
        InovioClient c = Harness.client();

        // Build a multi-leg order: authorize 100, capture 60, refund 10.
        TransactionResult order = Harness.seedOrder(c, "STATUS", false, "100.00");
        c.capture(order.orderRef(), Money.of("60.00", "USD"));
        c.refund(order.orderRef(), Money.of("10.00", "USD"));

        OrderStatus s = c.status(order.orderRef());

        Harness.show("legs", s.transactions().size());
        Harness.show("authorized", s.authorized().toWire());
        Harness.show("captured", s.captured().toWire());
        Harness.show("refunded", s.refunded().toWire());
        Harness.show("net", s.net().toWire() + "   (captured - refunded)");
        Harness.show("outstanding", s.outstanding().toWire() + "   (authorized - captured)");

        System.out.println("\n  legs:");
        for (TransactionResult leg : s.transactions()) {
            System.out.printf("    %-14s %-9s %s%n", leg.action(), leg.status(),
                leg.amount() == null ? "-" : leg.amount().toWire());
        }

        // You can also look an order up by YOUR id:
        //   c.status(Refs.xtlOrder("ORDER-555"));
    }
}
