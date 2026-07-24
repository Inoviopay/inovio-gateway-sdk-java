package com.inoviopay.gateway.request;

import com.inoviopay.gateway.model.Money;
import com.inoviopay.gateway.model.PaymentMethod;
import com.inoviopay.gateway.model.RequestParts.Address;
import com.inoviopay.gateway.model.RequestParts.Affiliate;
import com.inoviopay.gateway.model.RequestParts.BrowserData;
import com.inoviopay.gateway.model.RequestParts.Customer;
import com.inoviopay.gateway.model.RequestParts.Descriptor;
import com.inoviopay.gateway.model.RequestParts.Fees;
import com.inoviopay.gateway.model.RequestParts.Idempotency;
import com.inoviopay.gateway.model.RequestParts.LineItem;
import com.inoviopay.gateway.model.RequestParts.Metadata;
import com.inoviopay.gateway.model.RequestParts.PartialAuth;
import com.inoviopay.gateway.model.RequestParts.Recurring;
import com.inoviopay.gateway.model.RequestParts.RiskOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A card transaction request (object model §3.3). */
public final class TransactionRequest {

    public PaymentMethod paymentMethod;
    public final List<LineItem> lineItems = new ArrayList<>();
    public Money amount;
    public Customer customer;
    public Address billingAddress;
    public Address shippingAddress;
    public Descriptor descriptor;
    public RiskOptions risk;
    public PartialAuth partialAuth;
    public Idempotency idempotency;
    public Recurring recurring;
    public Fees fees;
    public Affiliate affiliate;
    public Metadata metadata;
    public String merchAcctId;
    public BrowserData browser;
    /** CCCREDIT + FORCE_CREDIT — a credit with no referenced original. */
    public boolean forceCredit;

    public TransactionRequest(PaymentMethod paymentMethod, LineItem... items) {
        this.paymentMethod = paymentMethod;
        this.lineItems.addAll(Arrays.asList(items));
    }

    public TransactionRequest idempotency(String xtlOrderId) {
        this.idempotency = new Idempotency(xtlOrderId);
        return this;
    }
}
