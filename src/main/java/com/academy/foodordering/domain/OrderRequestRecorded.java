package com.academy.foodordering.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A local post-save historical claim with typed identity references.
 *
 * The fact says that the process-local order repository save returned
 * normally. It is NOT a durable event, payment result, merchant acceptance,
 * delivered message or independent fact/event identifier.
 */
public record OrderRequestRecorded(
        OrderId orderIdentity,
        CustomerId customerIdentity,
        RestaurantId restaurantIdentity,
        Instant orderCreatedAt,
        Instant observedAfterSaveAt) {
    public OrderRequestRecorded {
        Objects.requireNonNull(orderIdentity, "orderIdentity");
        Objects.requireNonNull(customerIdentity, "customerIdentity");
        Objects.requireNonNull(restaurantIdentity, "restaurantIdentity");
        Objects.requireNonNull(orderCreatedAt, "orderCreatedAt");
        Objects.requireNonNull(observedAfterSaveAt, "observedAfterSaveAt");
    }
    public OrderRequestRecorded(UUID orderId, UUID customerId, String restaurantId,
                                Instant orderCreatedAt, Instant observedAfterSaveAt) {
        this(new OrderId(orderId), new CustomerId(customerId), new RestaurantId(restaurantId),
                orderCreatedAt, observedAfterSaveAt);
    }

    /** Backward-compatible raw identifier accessors used by existing code/tests. */
    public UUID orderId() { return orderIdentity.value(); }
    public UUID customerId() { return customerIdentity.value(); }
    public String restaurantId() { return restaurantIdentity.value(); }
}
