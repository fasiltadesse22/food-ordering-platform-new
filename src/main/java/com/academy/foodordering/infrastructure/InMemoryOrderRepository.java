package com.academy.foodordering.infrastructure;

import com.academy.foodordering.application.OrderRepository;
import com.academy.foodordering.domain.Order;
import com.academy.foodordering.domain.OrderId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * The only mutable owner of order-intake state in this demo. Identity keys
 * are domain types, and create-only semantics prevent accidental replacement.
 *
 * Atomic putIfAbsent is process-local only; records disappear on restart.
 * No legitimate order-update command, revision or history log exists yet.
 */
@Repository
public final class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentHashMap<OrderId, Order> data = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        Objects.requireNonNull(order, "order");
        if (data.putIfAbsent(order.orderIdentity(), order) != null) {
            throw new IllegalStateException("Existing order identity cannot be replaced by intake");
        }
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(data.get(new OrderId(id)));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id, "id")));
    }
}
