package com.academy.foodordering.domain;

import java.util.Objects;
import java.util.UUID;

/** Stable local identifier for an ANALYTICAL attempt; no provider integration. */
public record PaymentAttemptId(UUID value) {
    public PaymentAttemptId { Objects.requireNonNull(value, "value"); }
    public static PaymentAttemptId newId() { return new PaymentAttemptId(UUID.randomUUID()); }
}
