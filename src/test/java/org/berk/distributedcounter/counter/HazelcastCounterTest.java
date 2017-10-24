package org.berk.distributedcounter.counter;

import org.junit.Test;

import java.util.UUID;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HazelcastCounterTest {


    private final Counter counter = null; // TODO implement


    @Test
    public void shouldReturnNullForNonExisting() {
        assertNull(counter.getCount(UUID.randomUUID().toString()));
    }

    @Test
    public void shouldIncrementByOne()  {
        String eventId = UUID.randomUUID().toString();
        Long count = 10L;
        LongStream.range(0, count).forEach(value ->  counter.increment(eventId));
        assertEquals(count, counter.getCount(eventId));
    }

    @Test
    public void shouldIncrementByGivenAmount()  {
        String eventId = UUID.randomUUID().toString();
        assertNull(counter.getCount(eventId));
        Long count = 10L;
        int amount = 2;
        LongStream.range(0, count).forEach(value ->  counter.increment(eventId, amount));
        assertEquals(count * amount, counter.getCount(eventId).longValue());
    }


}