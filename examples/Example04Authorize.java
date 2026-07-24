import com.inoviopay.gateway.refs.Refs;
import com.inoviopay.gateway.result.TransactionResult;

import java.util.stream.Collectors;

/**
 * authorize() — CCAUTHORIZE
 *
 * <p>Places a hold without taking funds. Pair with capture() when you ship, or
 * reverse() to release the hold. Keep result.orderRef() — every follow-up
 * operation consumes it.
 */
public class Example04Authorize {
    public static void main(String[] a) {
        TransactionResult auth = Harness.client().authorize(Harness.request("AUTH", "10.00"));

        Harness.show("status", auth.status());
        Harness.show("order", auth.orderRef() == null ? "-" : auth.orderRef().poId());
        Harness.show("line items", auth.lineItemRefs().stream()
            .map(Refs.LineItemRef::poLiId).collect(Collectors.joining(", ")));
        Harness.show("avs", auth.avs() == null ? "-"
            : auth.avs().code() + " (" + auth.avs().classification() + ")");
        Harness.show("cvv", auth.cvv() == null ? "-"
            : auth.cvv().code() + " (" + auth.cvv().classification() + ")");

        // AVS 'partial' means some elements matched and some did not. Whether
        // that is acceptable is YOUR risk policy — the SDK reports, it does not
        // decide.
        if (auth.avs() != null && "partial".equals(auth.avs().classification())) {
            Harness.show("note", "partial AVS match — apply your risk policy");
        }
    }
}
