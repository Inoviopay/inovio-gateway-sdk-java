import com.inoviopay.gateway.errors.AuthenticationException;
import com.inoviopay.gateway.enums.TransactionStatus;
import com.inoviopay.gateway.result.TransactionResult;

/**
 * forceCredit() — CCCREDIT + FORCE_CREDIT
 *
 * <p>Pushes money to a card with NO original transaction to reference. Use it
 * for goodwill payments, or to refund an order taken outside the gateway.
 *
 * <p>Because nothing constrains the amount, merchant accounts must have this
 * enabled explicitly. If it is NOT enabled the gateway rejects at the API tier
 * with 104 "Invalid service action" — an AuthenticationException, not a
 * decline. Observed on live T1 with a standard test account.
 */
public class Example10ForceCredit {
    public static void main(String[] a) {
        try {
            TransactionResult r = Harness.client().forceCredit(Harness.request("FORCE", "10.00"));
            Harness.show("status", r.status());
            Harness.show("amount", r.amount() == null ? "-" : r.amount().toWire());
            if (r.status() == TransactionStatus.DECLINED) {
                Harness.show("service code", r.outcome().service().code()
                    + " \"" + r.outcome().service().advice() + "\"");
            }
        } catch (AuthenticationException e) {
            Harness.show("rejected", e.getMessage());
            Harness.show("cause", "FORCE_CREDIT is not enabled on this merchant account");
            Harness.show("fix", "ask Inovio support to enable it for the MID");
        }
    }
}
