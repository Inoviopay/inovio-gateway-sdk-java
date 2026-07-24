import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.model.RequestParts.LineItem;
import com.inoviopay.gateway.refs.Refs;
import com.inoviopay.gateway.result.OrderStatus;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * captureLineItem() — CCCAPTURE against one line item
 *
 * <p>For multi-item orders shipped separately: capture each line item as it
 * goes out, rather than capturing an amount against the whole order.
 *
 * <p>The gateway requires the PARENT ORDER and an amount alongside the
 * line-item id (spec §5.5.6) — passing the line-item ref alone is rejected.
 */
public class Example06CaptureLineItem {
    public static void main(String[] a) {
        InovioClient c = Harness.client();

        TransactionResult auth = c.authorize(Harness.request("LI",
            new LineItem(Harness.PRODUCT_ID, 1, Money.of("10.00", "USD")),
            new LineItem(Harness.PRODUCT_ID, 1, Money.of("5.00", "USD"))));
        Harness.show("authorized", auth.status() + " lineItems=" + auth.lineItemRefs().size());

        if (auth.lineItemRefs().isEmpty()) {
            Harness.show("note", "gateway returned no line-item refs for this order");
            return;
        }
        Refs.LineItemRef first = auth.lineItemRefs().get(0);
        // order + item + amount — all three are required.
        TransactionResult captured =
            c.captureLineItem(auth.orderRef(), first, Money.of("10.00", "USD"));
        Harness.show("captured item", first.poLiId() + " -> " + captured.status());

        OrderStatus s = c.status(auth.orderRef());
        Harness.show("outstanding", s.outstanding().toWire() + "   (the unshipped line item)");
    }
}
