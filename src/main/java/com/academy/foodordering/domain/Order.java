package com.academy.foodordering.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of one accepted PLATFORM intake. Repository is the sole
 * mutable owner of the process-local collection. There is no authorized update
 * command or versioned transition yet, and no merchant/payment provider effect.
 */
public record Order(
        OrderId orderIdentity, CustomerId customerIdentity, RestaurantId restaurantIdentity,
        List<OrderLine> lines, long quotedTotalCents, Instant createdAt) {
    public Order {
        Objects.requireNonNull(orderIdentity, "orderIdentity");
        Objects.requireNonNull(customerIdentity, "customerIdentity");
        Objects.requireNonNull(restaurantIdentity, "restaurantIdentity");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(createdAt, "createdAt");
        lines = List.copyOf(lines);
    }

    /**
     * Boundary-compatible constructor for existing tests and integration
     * adapters. Core domain state stores strongly typed identity values.
     */
    public Order(UUID id, UUID customerId, String restaurantId,
                 List<OrderLine> lines, long quotedTotalCents, Instant createdAt) {
        this(new OrderId(id), new CustomerId(customerId), new RestaurantId(restaurantId),
                lines, quotedTotalCents, createdAt);
    }

    /** Existing public transport-facing accessors intentionally retain their wire types. */
    public UUID id() { return orderIdentity.value(); }
    public UUID customerId() { return customerIdentity.value(); }
    public String restaurantId() { return restaurantIdentity.value(); }

    public String intakeStatus() { return "REQUESTED_AWAITING_RESTAURANT"; }
    public String restaurantDecision() { return "PENDING"; }
    public String paymentStatus() { return "NOT_STARTED"; }

    public record OrderLine(String menuItemCode, String name, int quantity,
                            long unitPriceCents, long lineTotalCents) {}
}
