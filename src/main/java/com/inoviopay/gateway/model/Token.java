package com.inoviopay.gateway.model;

import java.util.Objects;

/** Single-use ephemeral token from the token service -> {@code TOKEN_GUID}. */
public final class Token implements PaymentMethod {

    private final String guid;

    Token(String guid) {
        if (guid == null || guid.isEmpty()) {
            throw new IllegalArgumentException("token guid is required");
        }
        this.guid = guid;
    }

    @Override public String kind() { return "token"; }

    public String guid() { return guid; }

    @Override public boolean equals(Object o) {
        return o instanceof Token && guid.equals(((Token) o).guid);
    }

    @Override public int hashCode() { return Objects.hash(guid); }

    @Override public String toString() { return "Token{guid=" + guid + "}"; }
}
