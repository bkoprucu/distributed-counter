package org.berk.distributedcounter.counter;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class HazelcastTest {


    final Config config = new Config("test");
    protected final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(config);

    /**
     * Load the counter by sending lots of events
     * @param counter CounterManager to apply load
     * @param threads        Number of threads to run in parallel
     * @param eventCount     Number of events to send
     * @param eventIdPrefix  Evennt ids will be composed with eventIdPrefix + thread number
     */
    protected ExecutorService load(Counter counter, int threads, int eventCount, String eventIdPrefix) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(threadId -> executor.submit(() -> IntStream.range(0, eventCount)
                .forEach(value -> counter.increment(eventIdPrefix + threadId))));
        return executor;
    }

    /**
     * EventIds will be prefixed to identify and remove generated ones from the counter
     */
    protected String generateEventIdPrefix() {
        return "_test_" + getClass().getSimpleName() + System.nanoTime();
    }
}
