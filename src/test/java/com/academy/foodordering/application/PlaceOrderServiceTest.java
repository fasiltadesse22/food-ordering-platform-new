package com.academy.foodordering.application;

import com.academy.foodordering.domain.BusinessRejection;
import com.academy.foodordering.domain.Order;
import com.academy.foodordering.infrastructure.InMemoryMenuCatalog;
import com.academy.foodordering.infrastructure.InMemoryOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaceOrderServiceTest {
    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();
    private final PlaceOrderService service = new PlaceOrderService(
            new InMemoryMenuCatalog(), repository, Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneOffset.UTC));

    private PlaceOrderCommand command(String item, int qty) {
        return new PlaceOrderCommand(UUID.fromString("6f1595f8-02bc-4484-9800-810a98b611ee"),
                "cedar-kitchen", List.of(new PlaceOrderCommand.LineRequest(item, qty)));
    }

    @Test void placesRealIntakeWithoutClaimingMerchantOrPaymentSuccess() {
        Order order = service.place(command("lentil-bowl", 2));
        assertEquals("REQUESTED_AWAITING_RESTAURANT", order.intakeStatus());
        assertEquals("PENDING", order.restaurantDecision());
        assertEquals("NOT_STARTED", order.paymentStatus());
        assertEquals(2500, order.quotedTotalCents());
        assertEquals(Instant.parse("2026-11-08T00:00:00Z"), order.createdAt());
        assertEquals(order, repository.findById(order.id()).orElseThrow());
    }

    @Test void rejectsUnavailableItemWithoutCreatingAnOrder() {
        var rejection = assertThrows(BusinessRejection.class,
                () -> service.place(command("seasonal-special", 1)));
        assertEquals("ITEM_UNAVAILABLE", rejection.code());
    }

    @Test void rejectsUnknownRestaurant() {
        var invalid = new PlaceOrderCommand(UUID.randomUUID(), "not-here",
                List.of(new PlaceOrderCommand.LineRequest("lentil-bowl", 1)));
        assertEquals("RESTAURANT_NOT_FOUND",
                assertThrows(BusinessRejection.class, () -> service.place(invalid)).code());
    }

    @Test void rejectsUnknownItemAndDuplicateLine() {
        assertEquals("ITEM_NOT_FOUND",
                assertThrows(BusinessRejection.class, () -> service.place(command("unknown", 1))).code());
        var duplicate = new PlaceOrderCommand(UUID.randomUUID(), "cedar-kitchen",
                List.of(new PlaceOrderCommand.LineRequest("lentil-bowl", 1),
                        new PlaceOrderCommand.LineRequest("lentil-bowl", 2)));
        assertEquals("DUPLICATE_ITEM",
                assertThrows(BusinessRejection.class, () -> service.place(duplicate)).code());
    }

    @Test void rejectsInvalidQuantityEvenWithoutControllerValidation() {
        assertEquals("INVALID_ITEM",
                assertThrows(BusinessRejection.class, () -> service.place(command("lentil-bowl", 0))).code());
        assertEquals("INVALID_ITEM",
                assertThrows(BusinessRejection.class, () -> service.place(command("lentil-bowl", 21))).code());
    }

    @Test void repeatedPlaceOrderCreatesDistinctRecordsBecauseIdempotencyIsDeferred() {
        var first = service.place(command("lentil-bowl", 1));
        var second = service.place(command("lentil-bowl", 1));
        assertNotEquals(first.id(), second.id());
    }

    @Test void doesNotTrustAClientSuppliedPriceBecauseCommandHasNoPriceField() {
        var order = service.place(command("injera-combo", 2));
        assertEquals(3600, order.quotedTotalCents());
        assertEquals(1800, order.lines().getFirst().unitPriceCents());
    }
}
