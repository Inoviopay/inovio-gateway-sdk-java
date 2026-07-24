package com.inoviopay.gateway.model;

import java.util.Objects;

/** A previously vaulted card -> {@code PMT_ID} / {@code PMT_ID_XTL} (+ {@code CUST_ID}). */
public final class SavedCard implements PaymentMethod {

    private final String pmtId;
    private final String pmtIdXtl;
    private final String custId;

    SavedCard(String pmtId, String pmtIdXtl, String custId) {
        if ((pmtId == null || pmtId.isEmpty()) && (pmtIdXtl == null || pmtIdXtl.isEmpty())) {
            throw new IllegalArgumentException("savedCard requires one of pmtId or pmtIdXtl");
        }
        this.pmtId = pmtId;
        this.pmtIdXtl = pmtIdXtl;
        this.custId = custId;
    }

    @Override public String kind() { return "savedCard"; }

    public String pmtId() { return pmtId; }
    public String pmtIdXtl() { return pmtIdXtl; }
    public String custId() { return custId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedCard)) return false;
        SavedCard s = (SavedCard) o;
        return Objects.equals(pmtId, s.pmtId)
            && Objects.equals(pmtIdXtl, s.pmtIdXtl)
            && Objects.equals(custId, s.custId);
    }

    @Override public int hashCode() { return Objects.hash(pmtId, pmtIdXtl, custId); }

    @Override public String toString() {
        return "SavedCard{pmtId=" + pmtId + ", pmtIdXtl=" + pmtIdXtl + "}";
    }
}
