package com.academy.foodordering.application;

import com.academy.foodordering.domain.Order;
import com.academy.foodordering.domain.OrderRequestRecorded;
import java.util.Objects;

/**
 * An application's local decision, not an HTTP status or delivery guarantee.
 * Recorded means a process-local repository save returned normally.
 * Rejected means no successful intake was created by this handler.
 */
public sealed interface PlaceOrderOutcome
        permits PlaceOrderOutcome.Recorded, PlaceOrderOutcome.Rejected {
    record Recorded(Order order, OrderRequestRecorded fact) implements PlaceOrderOutcome {
        public Recorded {
            Objects.requireNonNull(order, "order");
            Objects.requireNonNull(fact, "fact");
            if (!order.orderIdentity().equals(fact.orderIdentity())
                    || !order.customerIdentity().equals(fact.customerIdentity())
                    || !order.restaurantIdentity().equals(fact.restaurantIdentity())) {
                throw new IllegalArgumentException("Fact must refer to the same order, customer and restaurant");
            }
        }
    }
    record Rejected(String code, String message) implements PlaceOrderOutcome {
        public Rejected {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }
}
