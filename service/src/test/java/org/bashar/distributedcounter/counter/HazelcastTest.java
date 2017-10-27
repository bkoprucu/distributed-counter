package org.bashar.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.bashar.distributedcounter.HazelcastConfig;
import org.bashar.distributedcounter.counter.CounterManager;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class HazelcastTest {

    protected final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(
            HazelcastConfig.getConfig("DistributedCounter_Test", 9300, Collections.singletonList("localhost")));

    /**
     * Load the counter by sending lots of events
     * @param counterManager CounterManager to apply load
     * @param threads        Number of threads to run in parallel
     * @param eventCount     Number of events to send
     * @param eventIdPrefix  Evennt ids will be composed with eventIdPrefix + thread number
     * @return
     */
    protected ExecutorService load(CounterManager counterManager, int threads, int eventCount, String eventIdPrefix) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(threadId -> {
            executor.submit(() -> IntStream.range(0, eventCount).forEach(value -> counterManager.increment(eventIdPrefix + threadId)));
        });
        return executor;
    }

    /**
     * EventIds will be prefixed to identify and remove generated ones from the counter
     */
    protected String generateEventIdPrefix() {
        return "_test_" + getClass().getSimpleName() + System.nanoTime();
    }
}
