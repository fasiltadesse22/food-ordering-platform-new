package com.academy.foodordering.api;

import com.academy.foodordering.application.MenuCatalog;
import com.academy.foodordering.application.OrderRepository;
import com.academy.foodordering.application.PlaceOrderCommand;
import com.academy.foodordering.application.PlaceOrderService;
import com.academy.foodordering.domain.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final PlaceOrderService service;
    private final OrderRepository repository;
    private final MenuCatalog catalog;

    public OrderController(PlaceOrderService service, OrderRepository repository, MenuCatalog catalog) {
        this.service = service;
        this.repository = repository;
        this.catalog = catalog;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        var command = new PlaceOrderCommand(request.customerId(), request.restaurantId(),
                request.items().stream().map(i -> new PlaceOrderCommand.LineRequest(
                        i.menuItemCode(), i.quantity())).toList());
        var order = service.place(command);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id()))
                .body(OrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        return repository.findById(id).map(OrderResponse::from)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/restaurants/{id}/menu")
    public ResponseEntity<MenuCatalog.RestaurantMenu> menu(@PathVariable String id) {
        return catalog.findRestaurant(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record PlaceOrderRequest(
            @NotNull UUID customerId,
            @NotBlank String restaurantId,
            @NotEmpty @Size(max = 20) List<@Valid PlaceOrderItem> items) {}

    public record PlaceOrderItem(
            @NotBlank String menuItemCode, @Min(1) @Max(20) int quantity) {}

    public record OrderResponse(
            UUID orderId, UUID customerId, String restaurantId, List<Order.OrderLine> items,
            long quotedTotalCents, String currency, String intakeStatus,
            String restaurantDecision, String paymentStatus, java.time.Instant createdAt,
            String meaning) {
        static OrderResponse from(Order order) {
            return new OrderResponse(order.id(), order.customerId(), order.restaurantId(),
                    order.lines(), order.quotedTotalCents(), "USD",
                    order.intakeStatus(), order.restaurantDecision(), order.paymentStatus(),
                    order.createdAt(),
                    "Platform recorded request; restaurant has not accepted and no payment was initiated");
        }
    }
}
