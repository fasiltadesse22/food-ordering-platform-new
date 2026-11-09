package com.academy.foodordering.application;

import com.academy.foodordering.domain.Order;
import com.academy.foodordering.infrastructure.InMemoryMenuCatalog;
import com.academy.foodordering.infrastructure.InMemoryOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaceOrderDecisionTest {
    private static final Clock FIXED = Clock.fixed(
            Instant.parse("2026-11-09T09:00:00Z"), ZoneOffset.UTC);
    private static final UUID CUSTOMER = UUID.fromString("6f1595f8-02bc-4484-9800-810a98b611ee");

    private PlaceOrderCommand command(String restaurant, String item, int qty) {
        return new PlaceOrderCommand(CUSTOMER, restaurant,
                List.of(new PlaceOrderCommand.LineRequest(item, qty)));
    }

    @Test void recordedOutcomeMatchesOrderSavedBeforeFactWasConstructed() {
        var store = new ObservingRepository();
        var service = new PlaceOrderService(new InMemoryMenuCatalog(), store, FIXED);
        var result = assertInstanceOf(PlaceOrderOutcome.Recorded.class,
                service.handle(command("cedar-kitchen", "lentil-bowl", 2)));
        assertEquals(1, store.saved.size());
        assertEquals(result.order(), store.saved.getFirst());
        assertEquals(result.order().id(), result.fact().orderId());
        assertEquals(result.order().customerId(), result.fact().customerId());
        assertEquals(result.order().restaurantId(), result.fact().restaurantId());
        assertEquals(result.order().createdAt(), result.fact().orderCreatedAt());
        assertEquals(FIXED.instant(), result.fact().observedAfterSaveAt());
        assertEquals("PENDING", result.order().restaurantDecision());
        assertEquals("NOT_STARTED", result.order().paymentStatus());
    }

    @Test void rejectedCommandsDoNotCreateSuccessfulFactsOrStoredIntakes() {
        var store = new ObservingRepository();
        var service = new PlaceOrderService(new InMemoryMenuCatalog(), store, FIXED);
        var cases = List.of(
                new Case(null, "CUSTOMER_REQUIRED"),
                new Case(command("unknown", "lentil-bowl", 1), "RESTAURANT_NOT_FOUND"),
                new Case(command("cedar-kitchen", "seasonal-special", 1), "ITEM_UNAVAILABLE"),
                new Case(command("cedar-kitchen", "missing", 1), "ITEM_NOT_FOUND"),
                new Case(command("cedar-kitchen", "lentil-bowl", 0), "INVALID_ITEM"),
                new Case(new PlaceOrderCommand(CUSTOMER, "cedar-kitchen",
                    List.of(new PlaceOrderCommand.LineRequest("lentil-bowl", 1),
                            new PlaceOrderCommand.LineRequest("lentil-bowl", 1))),
                    "DUPLICATE_ITEM")
        );
        for (var c : cases) {
            var decision = assertInstanceOf(PlaceOrderOutcome.Rejected.class,
                    service.handle(c.command()));
            assertEquals(c.code(), decision.code());
        }
        assertTrue(store.saved.isEmpty(), "Rejected commands must not record success");
    }

    @Test void retryAfterLostResponseStillCreatesTwoDistinctIntakes() {
        var store = new InMemoryOrderRepository();
        var service = new PlaceOrderService(new InMemoryMenuCatalog(), store, FIXED);
        var intent = command("cedar-kitchen", "lentil-bowl", 1);
        var first = assertInstanceOf(PlaceOrderOutcome.Recorded.class, service.handle(intent));
        var retry = assertInstanceOf(PlaceOrderOutcome.Recorded.class, service.handle(intent));
        assertNotEquals(first.order().id(), retry.order().id());
        assertNotEquals(first.fact().orderId(), retry.fact().orderId());
        assertTrue(store.findById(first.order().id()).isPresent());
        assertTrue(store.findById(retry.order().id()).isPresent());
    }

    @Test void unknownStorageEffectDoesNotBecomeBusinessRejectionOrSuccessFact() {
        var store = new ObservingRepository();
        store.throwAfterWrite = true;
        var service = new PlaceOrderService(new InMemoryMenuCatalog(), store, FIXED);
        var failure = assertThrows(IllegalStateException.class,
                () -> service.handle(command("cedar-kitchen", "lentil-bowl", 1)));
        assertEquals("simulated unknown outcome", failure.getMessage());
        assertEquals(1, store.saved.size(),
                "A storage effect can exist despite no returned success fact");
    }

    @Test void originalPlaceMethodRetainsBusinessAndHttpCompatibleContract() {
        var service = new PlaceOrderService(new InMemoryMenuCatalog(),
                new InMemoryOrderRepository(), FIXED);
        assertEquals(2500, service.place(command("cedar-kitchen", "lentil-bowl", 2))
                .quotedTotalCents());
        var rejected = assertThrows(com.academy.foodordering.domain.BusinessRejection.class,
                () -> service.place(command("cedar-kitchen", "seasonal-special", 1)));
        assertEquals("ITEM_UNAVAILABLE", rejected.code());
    }

    private record Case(PlaceOrderCommand command, String code) {}

    private static final class ObservingRepository implements OrderRepository {
        private final List<Order> saved = new ArrayList<>();
        private boolean throwAfterWrite;
        @Override public void save(Order order) {
            saved.add(order);
            if (throwAfterWrite) throw new IllegalStateException("simulated unknown outcome");
        }
        @Override public Optional<Order> findById(UUID id) {
            return saved.stream().filter(o -> o.id().equals(id)).findFirst();
        }
    }
}
