package org.berk.distributedcounter.counter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class HazelcastPNCounterTest extends CounterTestBase<HazelcastPNCounter> {

    @Override
    protected HazelcastPNCounter createInstance() {
        return new HazelcastPNCounter(hazelcastInstance, deduplicator);
    }


    @Test
    public void shouldRemove() {
        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, null).block();
        assertEquals(0L, counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldRemoveOnceForSameRequestId() {
        String requestId = UUID.randomUUID().toString();
        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, requestId).block();
        assertEquals(0L, counter.getCountAsync(eventId).block());

        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, requestId).block(); // Duplicate request, should be ignored
        assertEquals(5L, counter.getCountAsync(eventId).block());
    }

}