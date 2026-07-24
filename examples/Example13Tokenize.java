import com.inoviopay.gateway.Tokenize;
import com.inoviopay.gateway.model.PaymentMethods;
import com.inoviopay.gateway.request.TransactionRequest;
import com.inoviopay.gateway.result.TransactionResult;

import java.util.ArrayList;
import java.util.List;

/**
 * tokenize() — token_service.cfm
 *
 * <p>Exchanges a PAN for a single-use TOKEN_GUID that replaces PMT_NUMB on a
 * later transaction. A new token is required per transaction.
 *
 * <p>Needs a SITE KEY: a per-site HMAC secret from Inovio support, NOT your
 * gateway password. Without it the service answers error 121.
 *
 * <p>⚠️ This is a SERVER-SIDE call — the PAN passes through your
 * infrastructure, so you stay in PCI scope. The low-scope path is Hosted Fields.
 */
public class Example13Tokenize {
    public static void main(String[] a) {
        // Tokenize on the site that holds the HMAC key...
        Tokenize.Result t = Harness.tokenClient().tokenize(
            PaymentMethods.card(Harness.PAN, Harness.EXPIRY, Harness.CVV));

        Harness.show("token", t.token().guid());
        Harness.show("token req id", t.tokenReqId() == null ? "-" : t.tokenReqId());

        // BIN metadata is best-effort — blank when the BIN is not in the table.
        List<String> bits = new ArrayList<>();
        for (String b : new String[] {t.card().brand, t.card().type, t.card().bank}) {
            if (b != null) bits.add(b);
        }
        Harness.show("card", bits.isEmpty() ? "(BIN not found)" : String.join(" / ", bits));

        // The token replaces the PAN ONLY: expiry (and CVV) still travel with
        // it, which tokenize() carries forward for you.
        TransactionRequest req = Harness.request("TOK", "10.00");
        req.paymentMethod = t.token();
        TransactionResult sale = Harness.client().sale(req);
        Harness.show("sale with token", sale.status() + " order="
            + (sale.orderRef() == null ? "-" : sale.orderRef().poId()));
    }
}
