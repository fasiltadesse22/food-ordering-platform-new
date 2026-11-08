package com.academy.foodordering.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Captures accepted PLATFORM intake only. Merchant and payment decisions have not occurred. */
public record Order(
        UUID id, UUID customerId, String restaurantId, List<OrderLine> lines,
        long quotedTotalCents, Instant createdAt) {
    public Order {
        lines = List.copyOf(lines);
    }

    public String intakeStatus() {
        return "REQUESTED_AWAITING_RESTAURANT";
    }
    public String restaurantDecision() {
        return "PENDING";
    }
    public String paymentStatus() {
        return "NOT_STARTED";
    }

    public record OrderLine(String menuItemCode, String name, int quantity,
                            long unitPriceCents, long lineTotalCents) {}
}
