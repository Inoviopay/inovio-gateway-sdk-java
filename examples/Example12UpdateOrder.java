import com.inoviopay.gateway.InovioClient;
import com.inoviopay.gateway.model.RequestParts.Metadata;
import com.inoviopay.gateway.request.OrderUpdate;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * updateOrder() — CCTRANSUPDATE
 *
 * <p>Attaches data to an order after the fact. The main use is receipts, which
 * Appendix G/J compliance requires for negative-option and trial billing.
 */
public class Example12UpdateOrder {
    public static void main(String[] a) {
        InovioClient c = Harness.client();

        TransactionResult order = Harness.seedOrder(c, "UPDATE", false, "10.00");
        Harness.show("order", order.orderRef() == null ? "-" : order.orderRef().poId());

        OrderUpdate u = new OrderUpdate(
            "https://merchant.example.invalid/receipts/" + order.orderRef().poId());
        u.metadata = new Metadata();
        u.metadata.udf.put("01", "fulfilled-2026-07-23");
        u.metadata.udf.put("02", "warehouse-B");

        TransactionResult r = c.updateOrder(order.orderRef(), u);
        Harness.show("status", r.status());
        Harness.show("use", "receipts for MCC 5968 / Visa trial compliance");
    }
}
