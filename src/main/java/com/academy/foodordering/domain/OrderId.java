package com.academy.foodordering.domain;

import java.util.Objects;
import java.util.UUID;

/** Stable identity for a single order intake, not a request or a payment attempt. */
public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "OrderId value");
    }
    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
    public static OrderId parse(String raw) {
        return new OrderId(UUID.fromString(raw));
    }
    @Override public String toString() { return value.toString(); }
}
