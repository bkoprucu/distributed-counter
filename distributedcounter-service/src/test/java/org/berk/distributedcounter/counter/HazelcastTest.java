package org.berk.distributedcounter.counter;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class HazelcastTest {

    protected final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(new Config("test"));

    /**
     * Load the counter by sending lots of events
     * @param counter CounterManager to apply load
     * @param threads        Number of threads to run in parallel
     * @param eventCount     Number of events to send
     * @param countIdPrefix  Evennt ids will be composed with countIdPrefix + thread number
     */
    protected ExecutorService load(Counter counter, int threads, int eventCount, String countIdPrefix) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(threadId -> executor.submit(() -> IntStream.range(0, eventCount)
                .forEach(value -> counter.increment(countIdPrefix + threadId))));
        return executor;
    }

    /**
     * Return a unique String for testing
     */
    protected String randomCountId() {
        return "test_" + ThreadLocalRandom.current().nextLong();
    }
}
