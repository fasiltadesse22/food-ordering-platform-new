package com.academy.foodordering.infrastructure;

import com.academy.foodordering.domain.CustomerId;
import com.academy.foodordering.domain.Order;
import com.academy.foodordering.domain.OrderId;
import com.academy.foodordering.domain.RestaurantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests local repository ownership, not persistence durability or future update APIs. */
class OrderIdentityContinuityTest {
    private static final UUID ORDER = UUID.fromString("d34e4ce1-fbfd-43ba-8f86-8fb8c765993e");
    private static final UUID CUSTOMER = UUID.fromString("12623dad-a13d-4fc8-a352-1a8ae96c64ec");
    private static final Instant CREATED = Instant.parse("2026-11-10T07:00:00Z");

    private static Order intake(UUID id, long cents) {
        return new Order(new OrderId(id), new CustomerId(CUSTOMER),
                new RestaurantId("cedar-kitchen"), List.of(), cents, CREATED);
    }

    @Test void retrievingExistingOrderNeverCreatesNewBusinessIdentity() {
        var repo = new InMemoryOrderRepository();
        var order = intake(ORDER, 1250);
        repo.save(order);
        assertSame(order, repo.findById(ORDER).orElseThrow());
        assertSame(order, repo.findById(new OrderId(ORDER)).orElseThrow());
        assertEquals(order.orderIdentity(), repo.findById(new OrderId(ORDER))
                .orElseThrow().orderIdentity());
        assertTrue(repo.findById(OrderId.newId()).isEmpty());
    }

    @Test void accidentalReplacementUnderSameIdIsRejectedWithoutMutation() {
        var repo = new InMemoryOrderRepository();
        var original = intake(ORDER, 1250);
        repo.save(original);
        var misleadingReplacement = intake(ORDER, 9999);
        assertThrows(IllegalStateException.class, () -> repo.save(misleadingReplacement));
        assertSame(original, repo.findById(new OrderId(ORDER)).orElseThrow());
        assertEquals(1250, repo.findById(ORDER).orElseThrow().quotedTotalCents());
    }

    @Test void reSavingEvenSameSnapshotIsNotASecondCreate() {
        var repo = new InMemoryOrderRepository();
        var first = intake(ORDER, 1250);
        repo.save(first);
        assertThrows(IllegalStateException.class, () -> repo.save(first));
        assertEquals(first, repo.findById(ORDER).orElseThrow());
    }

    @Test void distinctBusinessIdsCanCoexistDespiteIdenticalAttributes() {
        var repo = new InMemoryOrderRepository();
        var first = intake(ORDER, 1250);
        var second = intake(UUID.fromString("54bcba68-4c4a-46cc-a7a6-13626a090ea9"), 1250);
        repo.save(first);
        repo.save(second);
        assertNotEquals(first.orderIdentity(), second.orderIdentity());
        assertEquals(first, repo.findById(first.orderIdentity()).orElseThrow());
        assertEquals(second, repo.findById(second.orderIdentity()).orElseThrow());
    }

    @Test void simultaneousCreateOfSameIdentityDoesNotOverwriteOrCreateVersion() throws Exception {
        var repo = new InMemoryOrderRepository();
        var first = intake(ORDER, 1250);
        var second = intake(ORDER, 9999);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var gate = new java.util.concurrent.CountDownLatch(1);
            var one = pool.submit(() -> { gate.await(); try { repo.save(first); return true; }
                catch (IllegalStateException expected) { return false; } });
            var two = pool.submit(() -> { gate.await(); try { repo.save(second); return true; }
                catch (IllegalStateException expected) { return false; } });
            gate.countDown();
            assertNotEquals(one.get(), two.get(), "Exactly one create wins for this key");
            var saved = repo.findById(new OrderId(ORDER)).orElseThrow();
            assertTrue(saved.equals(first) || saved.equals(second));
        } finally {
            pool.shutdownNow();
        }
    }
}
