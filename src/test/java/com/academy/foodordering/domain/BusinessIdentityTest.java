package com.academy.foodordering.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contract tests for real, typed domain identity without payment fixtures. */
class BusinessIdentityTest {
    private static final UUID ORDER = UUID.fromString("425f8e3b-2e62-4b02-84d8-4919bebac6df");
    private static final UUID CUSTOMER = UUID.fromString("9f91fb20-66dd-42cf-84ed-749c1792500d");
    private static final Instant TIME = Instant.parse("2026-11-10T07:00:00Z");

    @Test void identityValueEqualityIsStableAndNotReferenceEquality() {
        var first = new OrderId(ORDER);
        assertEquals(first, OrderId.parse(ORDER.toString()));
        assertEquals(ORDER.toString(), first.toString());
        assertNotSame(first, OrderId.parse(ORDER.toString()));
        assertEquals(new CustomerId(CUSTOMER), CustomerId.parse(CUSTOMER.toString()));
        assertEquals(new RestaurantId("cedar-kitchen"), new RestaurantId("cedar-kitchen"));
    }

    @Test void identityTypesCannotBeMistakenForOneAnother() {
        assertNotEquals(new OrderId(ORDER), new CustomerId(ORDER));
        assertNotEquals(new RestaurantId("cedar-kitchen"), new RestaurantId("Cedar Kitchen"));
        assertNotEquals(new RestaurantId("cedar-kitchen"), new RestaurantId("another-merchant"));
        assertTrue(OrderId.class.isRecord());
        assertTrue(CustomerId.class.isRecord());
        assertTrue(RestaurantId.class.isRecord());
    }

    @Test void invalidIdentifiersCannotEnterTypedDomainReferences() {
        assertThrows(NullPointerException.class, () -> new OrderId(null));
        assertThrows(NullPointerException.class, () -> new CustomerId(null));
        assertThrows(NullPointerException.class, () -> new RestaurantId(null));
        assertThrows(IllegalArgumentException.class, () -> new RestaurantId("   "));
        assertThrows(IllegalArgumentException.class, () -> OrderId.parse("not-a-uuid"));
    }

    @Test void stronglyTypedOrderPreservesLegacyHttpIdentifiers() {
        var order = new Order(new OrderId(ORDER), new CustomerId(CUSTOMER),
                new RestaurantId("cedar-kitchen"), List.of(), 1250, TIME);
        assertEquals(ORDER, order.id());
        assertEquals(CUSTOMER, order.customerId());
        assertEquals("cedar-kitchen", order.restaurantId());
        assertEquals(ORDER, order.orderIdentity().value());
        assertEquals(new CustomerId(CUSTOMER), order.customerIdentity());
        assertEquals(new RestaurantId("cedar-kitchen"), order.restaurantIdentity());
        assertEquals("PENDING", order.restaurantDecision());
        assertEquals("NOT_STARTED", order.paymentStatus());
    }

    @Test void immutableSnapshotCannotBeModifiedThroughItems() {
        var lines = new java.util.ArrayList<Order.OrderLine>();
        lines.add(new Order.OrderLine("lentil-bowl", "Lentil Bowl", 1, 1250, 1250));
        var order = new Order(ORDER, CUSTOMER, "cedar-kitchen", lines, 1250, TIME);
        lines.clear();
        assertEquals(1, order.lines().size());
        assertThrows(UnsupportedOperationException.class,
                () -> order.lines().add(new Order.OrderLine("other", "Other", 1, 1, 1)));
    }

    @Test void recordedFactReferencesTheExactTypedEntitiesButHasNoEventIdentity() {
        var fact = new OrderRequestRecorded(new OrderId(ORDER), new CustomerId(CUSTOMER),
                new RestaurantId("cedar-kitchen"), TIME, TIME.plusMillis(1));
        assertEquals(ORDER, fact.orderId());
        assertEquals(CUSTOMER, fact.customerId());
        assertEquals("cedar-kitchen", fact.restaurantId());
        assertEquals(new OrderId(ORDER), fact.orderIdentity());
        assertEquals(TIME.plusMillis(1), fact.observedAfterSaveAt());
        // Two wall-clock observations may move backwards under clock corrections.
        // Timestamps are not a reliable ordering/version guarantee.
        var correctedClock = new OrderRequestRecorded(
                new OrderId(ORDER), new CustomerId(CUSTOMER),
                new RestaurantId("cedar-kitchen"), TIME, TIME.minusMillis(1));
        assertTrue(correctedClock.observedAfterSaveAt().isBefore(correctedClock.orderCreatedAt()));
    }

    @Test void outcomeCannotClaimSuccessForMismatchedCustomerOrRestaurantIdentity() {
        var order = new Order(ORDER, CUSTOMER, "cedar-kitchen", List.of(), 0, TIME);
        var mismatchedCustomer = new OrderRequestRecorded(
                new OrderId(ORDER), new CustomerId(UUID.randomUUID()),
                new RestaurantId("cedar-kitchen"), TIME, TIME);
        assertThrows(IllegalArgumentException.class,
                () -> new com.academy.foodordering.application.PlaceOrderOutcome.Recorded(
                        order, mismatchedCustomer));
        var mismatchedRestaurant = new OrderRequestRecorded(
                new OrderId(ORDER), new CustomerId(CUSTOMER),
                new RestaurantId("other-merchant"), TIME, TIME);
        assertThrows(IllegalArgumentException.class,
                () -> new com.academy.foodordering.application.PlaceOrderOutcome.Recorded(
                        order, mismatchedRestaurant));
    }
}
