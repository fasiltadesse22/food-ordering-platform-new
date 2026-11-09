package com.academy.foodordering.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Local historical claim: an order-request intake save returned normally in
 * this one process. NOT merchant acceptance, payment, durable storage,
 * a persisted event ledger or published integration message.
 */
public record OrderRequestRecorded(
        UUID orderId,
        UUID customerId,
        String restaurantId,
        Instant orderCreatedAt,
        Instant observedAfterSaveAt) {
    public OrderRequestRecorded {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(restaurantId, "restaurantId");
        Objects.requireNonNull(orderCreatedAt, "orderCreatedAt");
        Objects.requireNonNull(observedAfterSaveAt, "observedAfterSaveAt");
    }
}
