package com.inoviopay.gateway.result;

import com.inoviopay.gateway.enums.AvsCodes;
import com.inoviopay.gateway.enums.CvvCodes;
import com.inoviopay.gateway.enums.TransactionStatus;
import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.refs.Refs;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a single gateway call (object model §3.5).
 *
 * <p>Two deliberate shapes, both load-bearing:
 *
 * <ol>
 *   <li><strong>No derived {@code approved}/{@code declined} flags.</strong>
 *       {@link #status()} is the only way to ask about outcome. Booleans invite
 *       {@code if (approved) ... else ...}, which silently treats
 *       {@code PENDING} as failure — exactly the card-shaped mental model the
 *       5-state lifecycle exists to prevent.
 *   <li><strong>Reference keys are flat</strong>, not nested in a {@code refs}
 *       bag. They are the most-touched fields on the result
 *       ({@code capture(result.orderRef(), ...)}).
 * </ol>
 */
public final class TransactionResult {

    public static final class Builder {
        TransactionStatus status = TransactionStatus.FAILED;
        String action = "";
        Refs.OrderRef orderRef;
        Refs.XtlOrderId xtlOrderRef;
        Refs.TransactionId transactionId;
        Refs.ReqId requestId;
        Refs.BatchId batchId;
        Refs.CustomerRef customerRef;
        Refs.SavedCardRef savedCardRef;
        Refs.MembershipRef membershipRef;
        List<Refs.LineItemRef> lineItemRefs = Collections.emptyList();
        Money amount;
        boolean settled;
        Conversion conversion;
        Outcome outcome = Outcome.empty();
        ServiceClassification serviceClassification;
        AvsResult avs;
        CvvResult cvv;
        CardInfo card;
        NextAction nextAction;
        Map<String, String> raw = Collections.emptyMap();

        public Builder status(TransactionStatus v) { this.status = v; return this; }
        public Builder action(String v) { this.action = v == null ? "" : v; return this; }
        public Builder orderRef(Refs.OrderRef v) { this.orderRef = v; return this; }
        public Builder xtlOrderRef(Refs.XtlOrderId v) { this.xtlOrderRef = v; return this; }
        public Builder transactionId(Refs.TransactionId v) { this.transactionId = v; return this; }
        public Builder requestId(Refs.ReqId v) { this.requestId = v; return this; }
        public Builder batchId(Refs.BatchId v) { this.batchId = v; return this; }
        public Builder customerRef(Refs.CustomerRef v) { this.customerRef = v; return this; }
        public Builder savedCardRef(Refs.SavedCardRef v) { this.savedCardRef = v; return this; }
        public Builder membershipRef(Refs.MembershipRef v) { this.membershipRef = v; return this; }
        public Builder lineItemRefs(List<Refs.LineItemRef> v) { this.lineItemRefs = v; return this; }
        public Builder amount(Money v) { this.amount = v; return this; }
        public Builder settled(boolean v) { this.settled = v; return this; }
        public Builder conversion(Conversion v) { this.conversion = v; return this; }
        public Builder outcome(Outcome v) { this.outcome = v; return this; }
        public Builder serviceClassification(ServiceClassification v) {
            this.serviceClassification = v; return this;
        }
        public Builder avs(AvsResult v) { this.avs = v; return this; }
        public Builder cvv(CvvResult v) { this.cvv = v; return this; }
        public Builder card(CardInfo v) { this.card = v; return this; }
        public Builder nextAction(NextAction v) { this.nextAction = v; return this; }
        public Builder raw(Map<String, String> v) { this.raw = v; return this; }

        public TransactionResult build() { return new TransactionResult(this); }
    }

    public static Builder builder() { return new Builder(); }

    private final Builder b;

    private TransactionResult(Builder b) {
        this.b = b;
    }

    /** APPROVED | DECLINED | PENDING | RUNNING | FAILED */
    public TransactionStatus status() { return b.status; }

    /** PENDING or RUNNING — a genuine grouping, not an alias for the status. */
    public boolean settling() { return b.status.isSettling(); }

    /** Echoed REQUEST_ACTION. */
    public String action() { return b.action; }

    public Refs.OrderRef orderRef() { return b.orderRef; }
    public Refs.XtlOrderId xtlOrderRef() { return b.xtlOrderRef; }
    public Refs.TransactionId transactionId() { return b.transactionId; }
    public Refs.ReqId requestId() { return b.requestId; }
    public Refs.BatchId batchId() { return b.batchId; }
    public Refs.CustomerRef customerRef() { return b.customerRef; }
    public Refs.SavedCardRef savedCardRef() { return b.savedCardRef; }
    public Refs.MembershipRef membershipRef() { return b.membershipRef; }
    public List<Refs.LineItemRef> lineItemRefs() { return b.lineItemRefs; }
    public Money amount() { return b.amount; }

    /**
     * The FACT of settlement. Written 0 at authorization and flipped later by
     * batch, except for settle-on-auth processors — so this is usually
     * {@code false} at response time and is <em>not</em> a failure signal.
     */
    public boolean settled() { return b.settled; }

    /**
     * Present ONLY when real currency conversion occurred. On a domestic
     * transaction the wire's "settled" amount is the auth amount echoed back, so
     * a block that was always present would mean nothing.
     */
    public Conversion conversion() { return b.conversion; }

    public Outcome outcome() { return b.outcome; }
    public ServiceClassification serviceClassification() { return b.serviceClassification; }
    public AvsResult avs() { return b.avs; }
    public CvvResult cvv() { return b.cvv; }
    public CardInfo card() { return b.card; }

    /** What must happen next when the status is PENDING (§4.1). */
    public NextAction nextAction() { return b.nextAction; }

    /** Escape hatch — every returned field, verbatim. */
    public Map<String, String> raw() {
        return Collections.unmodifiableMap(new HashMap<>(b.raw));
    }

    @Override public String toString() {
        return "TransactionResult{status=" + b.status + ", action=" + b.action
            + ", orderRef=" + b.orderRef + ", amount=" + b.amount + "}";
    }

    // ---- nested value types ------------------------------------------------

    /** One of the four layered response tiers (§1.3). */
    public static final class Tier {
        private final Integer code;
        private final String advice;
        private final String refField;

        public Tier(Integer code, String advice) { this(code, advice, null); }

        public Tier(Integer code, String advice, String refField) {
            this.code = code;
            this.advice = advice;
            this.refField = refField;
        }

        public Integer code() { return code; }
        public String advice() { return advice; }
        /** API tier only — names the offending field on validation failures. */
        public String refField() { return refField; }
    }

    /** The four independent tiers, outermost -> innermost. */
    public static final class Outcome {
        private final Tier api, service, processor, industry;

        public Outcome(Tier api, Tier service, Tier processor, Tier industry) {
            this.api = api;
            this.service = service;
            this.processor = processor;
            this.industry = industry;
        }

        static Outcome empty() {
            Tier t = new Tier(null, null);
            return new Outcome(t, t, t, t);
        }

        public Tier api() { return api; }
        /** The decline taxonomy lives here. */
        public Tier service() { return service; }
        public Tier processor() { return processor; }
        public Tier industry() { return industry; }
    }

    public static final class ServiceClassification {
        private final boolean retryable, stopRecurring, terminal, approval;

        public ServiceClassification(boolean retryable, boolean stopRecurring,
                                     boolean terminal, boolean approval) {
            this.retryable = retryable;
            this.stopRecurring = stopRecurring;
            this.terminal = terminal;
            this.approval = approval;
        }

        public boolean retryable() { return retryable; }
        public boolean stopRecurring() { return stopRecurring; }
        public boolean terminal() { return terminal; }
        public boolean approval() { return approval; }
    }

    public static final class Conversion {
        private final Money amount;
        private final String exchangeRate;

        public Conversion(Money amount, String exchangeRate) {
            this.amount = amount;
            this.exchangeRate = exchangeRate;
        }

        public Money amount() { return amount; }
        public String exchangeRate() { return exchangeRate; }
    }

    public static final class AvsResult {
        private final AvsCodes.Info info;
        private final String raw;

        public AvsResult(AvsCodes.Info info, String raw) { this.info = info; this.raw = raw; }

        public String code() { return info.code(); }
        public String description() { return info.description(); }
        public String cardNetwork() { return info.cardNetwork(); }
        /** DERIVED: positive | partial | negative | neutral. */
        public String classification() { return info.classification(); }
        public String raw() { return raw; }
    }

    public static final class CvvResult {
        private final CvvCodes.Info info;
        private final String raw;

        public CvvResult(CvvCodes.Info info, String raw) { this.info = info; this.raw = raw; }

        public String code() { return info.code(); }
        public String description() { return info.description(); }
        /** DERIVED: match | no_match | neutral. */
        public String classification() { return info.classification(); }
        public String raw() { return raw; }
    }

    public static final class AccountUpdater {
        public final String description, date, newExpiry, newLast4;

        public AccountUpdater(String description, String date, String newExpiry, String newLast4) {
            this.description = description;
            this.date = date;
            this.newExpiry = newExpiry;
            this.newLast4 = newLast4;
        }
    }

    public static final class CardInfo {
        public String brand, detail, type, cardClass, country, bank, balance, last4;
        public Boolean prepaid;
        public Integer networkTokenUsed;
        public AccountUpdater accountUpdater;
    }

    /** What must happen next when PENDING. */
    public static final class NextAction {
        public final String kind;
        public String url, barcode, token, redirectUrl, jwt, procTransId, pareq;

        public NextAction(String kind) { this.kind = kind; }

        public String kind() { return kind; }
    }
}
