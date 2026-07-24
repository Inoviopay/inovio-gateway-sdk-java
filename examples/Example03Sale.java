import com.inoviopay.gateway.result.TransactionResult;

/**
 * sale() — CCAUTHCAP
 *
 * <p>Authorize and capture in one step. The common case for immediate
 * fulfilment. Use authorize() + capture() instead when you ship later.
 *
 * <p>A DECLINE IS NOT AN ERROR — it returns normally with status DECLINED.
 */
public class Example03Sale {
    public static void main(String[] a) {
        TransactionResult r = Harness.client().sale(Harness.request("SALE", "10.00"));

        Harness.show("status", r.status());
        Harness.show("order", r.orderRef() == null ? "-" : r.orderRef().poId());
        Harness.show("amount", r.amount() == null ? "-"
            : r.amount().toWire() + " " + r.amount().currency());
        Harness.show("card", (r.card() == null ? "?" : r.card().brand)
            + " ****" + (r.card() == null ? "?" : r.card().last4));

        switch (r.status()) {
            case APPROVED:
                Harness.show("next", "fulfil the order");
                break;
            case DECLINED:
                // The service tier carries the decline taxonomy dunning needs.
                boolean retry = r.serviceClassification() != null
                    && r.serviceClassification().retryable();
                Harness.show("next", retry ? "retry later" : "do not retry");
                break;
            case PENDING:
                Harness.show("next", "complete "
                    + (r.nextAction() == null ? "?" : r.nextAction().kind()));
                break;
            default:
                Harness.show("next", "inspect result.outcome()");
        }
    }
}
