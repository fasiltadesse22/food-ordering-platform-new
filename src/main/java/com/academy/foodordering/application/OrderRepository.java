package com.academy.foodordering.application;

import com.academy.foodordering.domain.Order;
import java.util.Optional;
import java.util.UUID;

/** Port isolating intentionally non-durable storage from the use case. */
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(UUID id);
}
