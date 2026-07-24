package com.inoviopay.gateway.refs;

/**
 * Typed identity wrappers (object model §3.4).
 *
 * <p>There is no single transaction handle in the gateway — different
 * follow-ups consume different keys (§1.4). Distinct types make it impossible
 * to hand {@code capture()} a customer id by mistake.
 *
 * <p>These would be {@code record}s on Java 17+; the Java 11 baseline
 * (decision D3) spells them out.
 */
public final class Refs {

    private Refs() {}

    private abstract static class StringRef {
        final String value;

        StringRef(String value, String name) {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException(name + " is required");
            }
            this.value = value;
        }

        @Override public boolean equals(Object o) {
            return o != null && o.getClass() == getClass() && value.equals(((StringRef) o).value);
        }

        @Override public int hashCode() { return value.hashCode(); }

        @Override public String toString() {
            return getClass().getSimpleName() + "{" + value + "}";
        }
    }

    /** Gateway order id (PO_ID) -> REQUEST_REF_PO_ID */
    public static final class OrderRef extends StringRef {
        OrderRef(String v) { super(v, "poId"); }
        public String poId() { return value; }
    }

    /** Merchant order id (XTL_ORDER_ID) -> REQUEST_REF_PO_ID_XTL; idempotency key */
    public static final class XtlOrderId extends StringRef {
        XtlOrderId(String v) { super(v, "xtlOrderId"); }
        public String value() { return value; }
    }

    /** Gateway line-item id (PO_LI_ID_n) -> REQUEST_REF_PO_LI_ID */
    public static final class LineItemRef extends StringRef {
        LineItemRef(String v) { super(v, "poLiId"); }
        public String poLiId() { return value; }
    }

    public static final class TransactionId extends StringRef {
        TransactionId(String v) { super(v, "transactionId"); }
        public String value() { return value; }
    }

    public static final class ReqId extends StringRef {
        ReqId(String v) { super(v, "reqId"); }
        public String value() { return value; }
    }

    public static final class BatchId extends StringRef {
        BatchId(String v) { super(v, "batchId"); }
        public String value() { return value; }
    }

    /** Customer (CUST_ID / XTL_CUST_ID) */
    public static final class CustomerRef {
        private final String custId;
        private final String xtlCustId;

        CustomerRef(String custId, String xtlCustId) {
            this.custId = custId;
            this.xtlCustId = xtlCustId;
        }

        public String custId() { return custId; }
        public String xtlCustId() { return xtlCustId; }
    }

    /** Saved card (PMT_ID / PMT_ID_XTL) */
    public static final class SavedCardRef {
        private final String pmtId;
        private final String pmtIdXtl;

        SavedCardRef(String pmtId, String pmtIdXtl) {
            this.pmtId = pmtId;
            this.pmtIdXtl = pmtIdXtl;
        }

        public String pmtId() { return pmtId; }
        public String pmtIdXtl() { return pmtIdXtl; }
    }

    /** Membership (MBSHP_ID / MBSHP_ID_XTL) -> REQUEST_REF_MBSHP_ID */
    public static final class MembershipRef {
        private final String mbshpId;
        private final String mbshpIdXtl;

        MembershipRef(String mbshpId, String mbshpIdXtl) {
            this.mbshpId = mbshpId;
            this.mbshpIdXtl = mbshpIdXtl;
        }

        public String mbshpId() { return mbshpId; }
        public String mbshpIdXtl() { return mbshpIdXtl; }
    }

    public static OrderRef order(String poId) { return new OrderRef(poId); }
    public static XtlOrderId xtlOrder(String v) { return new XtlOrderId(v); }
    public static LineItemRef lineItem(String v) { return new LineItemRef(v); }
    public static TransactionId transaction(String v) { return new TransactionId(v); }
    public static ReqId req(String v) { return new ReqId(v); }
    public static BatchId batch(String v) { return new BatchId(v); }
    public static CustomerRef customer(String custId, String xtlCustId) {
        return new CustomerRef(custId, xtlCustId);
    }
    public static SavedCardRef savedCard(String pmtId, String pmtIdXtl) {
        return new SavedCardRef(pmtId, pmtIdXtl);
    }
    public static MembershipRef membership(String mbshpId, String mbshpIdXtl) {
        return new MembershipRef(mbshpId, mbshpIdXtl);
    }
}
