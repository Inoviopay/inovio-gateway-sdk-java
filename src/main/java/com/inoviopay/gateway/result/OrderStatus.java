package com.inoviopay.gateway.result;

import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.refs.Refs;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OrderStatus — the order is the aggregation root (object model §3.6).
 *
 * <p>Partial capture, multi-capture, refund and void are <em>separate</em>
 * transaction rows sharing a {@code PO_ID}, not modifications of the original.
 * So net position is an order-level question, and one {@code TransactionResult}
 * cannot answer "what did this order actually settle for". These figures mirror
 * the gateway's own {@code BATCH_PKG} sibling-sum keyed on {@code PO_ID}.
 *
 * <p>This makes {@code status()} the reconciliation primitive, not merely the
 * timeout-recovery primitive.
 */
public final class OrderStatus {

    private final Refs.OrderRef ref;
    private final Refs.XtlOrderId xtlRef;
    private final List<TransactionResult> transactions;
    private final Money authorized;
    private final Money captured;
    private final Money refunded;
    private final Money net;
    private final Money outstanding;
    private final boolean settled;
    private final Map<String, String> raw;

    public OrderStatus(Refs.OrderRef ref, Refs.XtlOrderId xtlRef,
                       List<TransactionResult> transactions, Money authorized,
                       Money captured, Money refunded, Money net, Money outstanding,
                       boolean settled, Map<String, String> raw) {
        this.ref = ref;
        this.xtlRef = xtlRef;
        this.transactions = Collections.unmodifiableList(transactions);
        this.authorized = authorized;
        this.captured = captured;
        this.refunded = refunded;
        this.net = net;
        this.outstanding = outstanding;
        this.settled = settled;
        this.raw = raw;
    }

    public Refs.OrderRef ref() { return ref; }
    public Refs.XtlOrderId xtlRef() { return xtlRef; }
    /** Every leg against this PO_ID: auth, captures, refunds, voids. */
    public List<TransactionResult> transactions() { return transactions; }
    public Money authorized() { return authorized; }
    public Money captured() { return captured; }
    public Money refunded() { return refunded; }
    /** captured - refunded */
    public Money net() { return net; }
    /** authorized - captured (uncaptured balance) */
    public Money outstanding() { return outstanding; }
    public boolean settled() { return settled; }
    public Map<String, String> raw() { return raw; }
}
