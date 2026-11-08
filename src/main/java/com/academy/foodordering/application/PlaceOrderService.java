package com.academy.foodordering.application;

import com.academy.foodordering.domain.BusinessRejection;
import com.academy.foodordering.domain.Order;
import com.academy.foodordering.domain.Order.OrderLine;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * First vertical slice: order REQUEST INTAKE. Does not accept a restaurant
 * fulfillment decision, perform authorization, or process a provider payment.
 */
@Service
public class PlaceOrderService {
    private final MenuCatalog catalog;
    private final OrderRepository repository;
    private final Clock clock;

    public PlaceOrderService(MenuCatalog catalog, OrderRepository repository) {
        this(catalog, repository, Clock.systemUTC());
    }

    // Package/public constructor allows deterministic unit tests.
    public PlaceOrderService(MenuCatalog catalog, OrderRepository repository, Clock clock) {
        this.catalog = catalog;
        this.repository = repository;
        this.clock = clock;
    }

    public Order place(PlaceOrderCommand command) {
        if (command == null || command.customerId() == null) {
            throw new BusinessRejection("CUSTOMER_REQUIRED", "A customer identifier is required");
        }
        if (command.restaurantId() == null || command.restaurantId().isBlank()) {
            throw new BusinessRejection("RESTAURANT_REQUIRED", "A restaurant identifier is required");
        }
        if (command.items() == null || command.items().isEmpty() || command.items().size() > 20) {
            throw new BusinessRejection("INVALID_BASKET", "Basket must contain between 1 and 20 distinct items");
        }
        var menu = catalog.findRestaurant(command.restaurantId())
                .orElseThrow(() -> new BusinessRejection("RESTAURANT_NOT_FOUND",
                        "Unknown restaurant: " + command.restaurantId()));
        var lookup = menu.items().stream().collect(java.util.stream.Collectors.toMap(
                MenuCatalog.MenuItem::code, item -> item));
        Set<String> distinct = new HashSet<>();
        List<OrderLine> lines = new ArrayList<>();
        long total = 0;
        for (var item : command.items()) {
            if (item == null || item.menuItemCode() == null || item.menuItemCode().isBlank()
                    || item.quantity() < 1 || item.quantity() > 20) {
                throw new BusinessRejection("INVALID_ITEM", "Each line needs an item code and quantity from 1 to 20");
            }
            if (!distinct.add(item.menuItemCode())) {
                throw new BusinessRejection("DUPLICATE_ITEM", "Use one line per menu item");
            }
            var listed = lookup.get(item.menuItemCode());
            if (listed == null) {
                throw new BusinessRejection("ITEM_NOT_FOUND",
                        "Item is not on this restaurant menu: " + item.menuItemCode());
            }
            if (!listed.available()) {
                throw new BusinessRejection("ITEM_UNAVAILABLE", "Item is marked unavailable: " + item.menuItemCode());
            }
            final long lineTotal;
            try {
                lineTotal = Math.multiplyExact(listed.priceCents(), item.quantity());
                total = Math.addExact(total, lineTotal);
            } catch (ArithmeticException ex) {
                throw new BusinessRejection("INVALID_TOTAL", "Basket total exceeds supported range");
            }
            lines.add(new OrderLine(listed.code(), listed.name(), item.quantity(),
                    listed.priceCents(), lineTotal));
        }
        // UUID identifies intake record; repeated POSTs have NO idempotency promise.
        var order = new Order(UUID.randomUUID(), command.customerId(), menu.code(),
                lines, total, Instant.now(clock));
        repository.save(order);
        return order;
    }
}
