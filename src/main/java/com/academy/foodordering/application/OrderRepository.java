package com.academy.foodordering.application;

import com.academy.foodordering.domain.Order;
import com.academy.foodordering.domain.OrderId;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for process-local order intake state.
 * save is CREATE-ONLY: the existing identity must never be silently replaced.
 * No arbitrary entity update or optimistic-concurrency protocol exists yet.
 */
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(UUID id);

    /** Domain-facing lookup with semantically typed identity. */
    default Optional<Order> findById(OrderId id) {
        return findById(id.value());
    }
}
