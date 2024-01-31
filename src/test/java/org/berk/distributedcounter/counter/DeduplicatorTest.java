package org.berk.distributedcounter.counter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicatorTest extends HazelcastTest {
    
    private final Deduplicator deduplicator = new Deduplicator(hazelcastInstance);
    
    @Test
    void shouldCallOnDuplicateSupplier() {
        String requestId = UUID.randomUUID().toString();
        assertEquals("success", deduplicator.deduplicate(requestId, () -> "success", () -> "onDuplicate"));
        assertEquals("onDuplicate", deduplicator.deduplicate(requestId, () -> "success", () -> "onDuplicate"));
        assertEquals("onDuplicate", deduplicator.deduplicate(requestId, () -> "success", () -> "onDuplicate"));
    }

    @Test
    void shouldReturnNullWhenNoDuplicateSupplier() {
        String requestId = UUID.randomUUID().toString();
        assertEquals("success", deduplicator.deduplicate(requestId, () -> "success", null));
        assertNull(deduplicator.deduplicate(requestId, () -> "success", null));
    }

}