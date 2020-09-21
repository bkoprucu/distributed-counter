package org.berk.distributedcounter.counter;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class HazelcastTest {

    protected final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(new Config("test"));

    /**
     * Load the counter by sending lots of events
     * @param counter CounterManager to apply load
     * @param eventCount     Number of events to send
     */
    protected CompletableFuture<Void> load(Counter counter, int eventCount, String countIdPrefix) {
        return CompletableFuture.allOf(IntStream.range(0, eventCount)
                .mapToObj(i -> counter.incrementAsync(countIdPrefix + i)).toArray(CompletableFuture[]::new));
    }

    /**
     * Return a unique String for testing
     */
    protected String randomCountId() {
        return "test_" + ThreadLocalRandom.current().nextLong();
    }
}
