package com.academy.foodordering.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A customer reference; its presence does NOT authenticate the HTTP caller.
 * Identity is deliberately distinct from order and payment-attempt identities.
 */
public record CustomerId(UUID value) {
    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value");
    }
    public static CustomerId parse(String raw) {
        return new CustomerId(UUID.fromString(raw));
    }
    @Override public String toString() { return value.toString(); }
}
