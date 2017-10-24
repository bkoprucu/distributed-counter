package org.berk.distributedcounter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.berk.distributedcounter.rest.api.EventCount;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HazelcastCounterTest {

    final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(
            HazelcastConfig.newConfig("DistributedCounter_Test", 9300, Collections.singletonList("localhost")));

    private final HazelcastCounter counter = new HazelcastCounter(hazelcastInstance);


    @Test
    public void shouldReturnNullForNonExisting() {
        assertNull(counter.getCount(UUID.randomUUID().toString()));
    }

    @Before
    public void setUp() {
        counter.clear();
    }

    @Test
    public void shouldIncrementByOne() {
        String eventId = UUID.randomUUID().toString();
        Long count = 10L;
        LongStream.range(0, count).forEach(value -> counter.increment(eventId, null));
        assertEquals(count, counter.getCount(eventId));
    }

    @Test
    public void shouldIncrementByGivenAmount() {
        String eventId = UUID.randomUUID().toString();
        assertNull(counter.getCount(eventId));
        long count = 10L;
        int amount = 2;
        LongStream.range(0, count).forEach(value -> counter.increment(eventId, amount, null));
        assertEquals(count * amount, counter.getCount(eventId).longValue());
    }

    @Test
    public void shouldIncrementOnceByOneForSameRequestId() {
        String eventId = UUID.randomUUID().toString();
        String requestId = "x";
        assertNull(counter.getCount(eventId));
        long count = 10L;
        LongStream.range(0, count).forEach(value -> counter.increment(eventId, requestId));
        assertEquals(1, counter.getCount(eventId).longValue());
    }

    @Test
    public void shouldIncrementOnceByGivenAmountAndSameRequestId() {
        String eventId = UUID.randomUUID().toString();
        String requestId = "1";
        assertNull(counter.getCount(eventId));
        long count = 10L;
        int amount = 2;
        LongStream.range(0, count).forEach(value -> counter.increment(eventId, amount, requestId));
        assertEquals(2, counter.getCount(eventId).longValue());
    }

    @Test
    public void shouldReturnSize() {
        assertEquals(0, counter.getSize());
        counter.increment("one", null);
        counter.increment("two", null);
        assertEquals(2, counter.getSize());
        counter.remove("one", null);
        assertEquals(1, counter.getSize());
    }

    @Test
    public void shouldListCounters() {
        assertTrue(counter.getCounts().isEmpty());
        List<EventCount> counts = Arrays.asList(new EventCount("one", 1L), new EventCount("two", 2L));
        counter.increment("one", null);
        counter.increment("two", 2, null);
        assertEquals(counts, counter.getCounts());
    }

    @Test
    public void shouldRemove() {
        String eventId = UUID.randomUUID().toString();
        counter.increment(eventId, 5, null);
        assertEquals(5L, counter.getCount(eventId).longValue());
        counter.remove(eventId, null);
        assertNull(counter.getCount(eventId));
    }
}