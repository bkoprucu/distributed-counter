package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.berk.distributedcounter.Counter;
import org.berk.distributedcounter.hazelcast.HazelcastConfig;
import org.berk.distributedcounter.hazelcast.HazelcastCounter;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HazelcastCounterTest {

    final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(
            HazelcastConfig.getConfig("DistributedCounter_Test", 9300, Collections.singletonList("localhost")));

    private final Counter counter = new HazelcastCounter(hazelcastInstance);


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