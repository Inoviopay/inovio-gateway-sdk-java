package com.inoviopay.gateway.request;

import com.inoviopay.gateway.model.RequestParts.Metadata;

/** CCTRANSUPDATE payload — receipts attached post-hoc (Appendix G compliance). */
public final class OrderUpdate {
    public String receipt;
    public Metadata metadata;

    public OrderUpdate() {}

    public OrderUpdate(String receipt) { this.receipt = receipt; }
}
