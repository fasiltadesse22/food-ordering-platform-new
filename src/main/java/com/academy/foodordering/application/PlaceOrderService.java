package com.academy.foodordering.application;

import com.academy.foodordering.domain.BusinessRejection;
import com.academy.foodordering.domain.Order;
import com.academy.foodordering.domain.OrderRequestRecorded;
import com.academy.foodordering.domain.OrderId;
import com.academy.foodordering.domain.CustomerId;
import com.academy.foodordering.domain.RestaurantId;
import com.academy.foodordering.domain.Order.OrderLine;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * First vertical slice: order REQUEST INTAKE. Does not accept a restaurant
 * fulfillment decision, perform authorization, or process a provider payment.
 */
@Service
public class PlaceOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(PlaceOrderService.class);
    private final MenuCatalog catalog;
    private final OrderRepository repository;
    private final Clock clock;

    @Autowired
    public PlaceOrderService(MenuCatalog catalog, OrderRepository repository) {
        this(catalog, repository, Clock.systemUTC());
    }

    // Package/public constructor allows deterministic unit tests.
    public PlaceOrderService(MenuCatalog catalog, OrderRepository repository, Clock clock) {
        this.catalog = catalog;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Explicit local decision: success fact only AFTER save returns normally.
     * This does not prove durability, publication, or remote delivery.
     * Unexpected storage errors propagate because a write might have occurred.
     */
    public PlaceOrderOutcome handle(PlaceOrderCommand command) {
        final Order order;
        try {
            order = validateCreateAndStore(command);
        } catch (BusinessRejection rejection) {
            return new PlaceOrderOutcome.Rejected(rejection.code(), rejection.getMessage());
        }
        var fact = new OrderRequestRecorded(order.orderIdentity(), order.customerIdentity(),
                order.restaurantIdentity(), order.createdAt(), Instant.now(clock));
        return new PlaceOrderOutcome.Recorded(order, fact);
    }

    /** Preserve original UC-001 service/HTTP behavior for existing clients. */
    public Order place(PlaceOrderCommand command) {
        return switch (handle(command)) {
            case PlaceOrderOutcome.Recorded result -> result.order();
            case PlaceOrderOutcome.Rejected rejection ->
                    throw new BusinessRejection(rejection.code(), rejection.message());
        };
    }

    private Order validateCreateAndStore(PlaceOrderCommand command) {
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
        var order = new Order(OrderId.newId(), new CustomerId(command.customerId()),
                new RestaurantId(menu.code()), lines, total, Instant.now(clock));
        repository.save(order);
        LOG.info("event=order_intake_recorded orderId={} restaurantId={} itemCount={} quotedTotalCents={}",
                order.id(), order.restaurantId(), order.lines().size(), order.quotedTotalCents());
        return order;
    }
}
