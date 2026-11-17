package com.academy.foodordering.domain;

import java.util.Objects;

/**
 * Pure inspection view; the three states are not an atomic transaction.
 * Payment and merchant facts are conceptual until external authority exists.
 */
public record LifecycleSnapshot(OrderLifecycle order, RestaurantDecisionLifecycle merchant,
                                PaymentAttemptLifecycle payment) {
    public LifecycleSnapshot {
        Objects.requireNonNull(order); Objects.requireNonNull(merchant); Objects.requireNonNull(payment);
        if (!order.orderId().equals(merchant.orderId()) || !order.orderId().equals(payment.orderId()))
            throw new IllegalArgumentException("Lifecycle references must point to the same OrderId");
    }
    public boolean awaitingMerchantDespiteAuthorization() {
        return merchant.state() == RestaurantDecisionLifecycle.State.PENDING &&
                payment.state() == PaymentAttemptLifecycle.State.AUTHORIZED;
    }
    public boolean merchantRejectedButFinancialRemediationPending() {
        return merchant.state() == RestaurantDecisionLifecycle.State.REJECTED &&
                payment.state() == PaymentAttemptLifecycle.State.REFUND_PENDING;
    }
}
