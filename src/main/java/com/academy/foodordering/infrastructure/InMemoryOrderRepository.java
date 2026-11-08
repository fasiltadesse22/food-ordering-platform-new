package com.academy.foodordering.infrastructure;

import com.academy.foodordering.application.OrderRepository;
import com.academy.foodordering.domain.Order;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Process-local, volatile demo store. Data disappears on process restart. */
@Repository
public final class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentHashMap<UUID, Order> data = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        data.put(order.id(), order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }
}
