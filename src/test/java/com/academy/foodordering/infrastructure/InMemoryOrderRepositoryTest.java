package com.academy.foodordering.infrastructure;

import com.academy.foodordering.domain.Order;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryOrderRepositoryTest {
    @Test void storesAndRetrievesIntakeRecordsInSameProcess() {
        var repo = new InMemoryOrderRepository();
        var id = UUID.randomUUID();
        var order = new Order(id, UUID.randomUUID(), "cedar-kitchen", List.of(), 0, Instant.now());
        assertTrue(repo.findById(id).isEmpty());
        repo.save(order);
        assertEquals(order, repo.findById(id).orElseThrow());
        assertTrue(repo.findById(UUID.randomUUID()).isEmpty());
    }
}
