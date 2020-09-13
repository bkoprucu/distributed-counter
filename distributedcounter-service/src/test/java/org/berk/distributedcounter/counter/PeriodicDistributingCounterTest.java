package org.berk.distributedcounter.counter;


import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PeriodicDistributingCounterTest extends HazelcastTest {

    private final PeriodicDistributingCounter<String> counter = new PeriodicDistributingCounter<>(hazelcastInstance, Duration.ofMillis(500));


    @BeforeEach
    public void setUp()  {
        counter.init();
    }

    @Test
    public void should_count()  {
        String eventId = UUID.randomUUID().toString();
        int count = 100;
        IntStream.range(0, count).forEach(value ->  counter.increment(eventId));
        Awaitility.await().atMost(counter.getSyncInterval().multipliedBy(5))
                .until(() -> !counter.isSyncInProgress() && counter.getCount(eventId).getCountVal() > 0);
        assertEquals(count, counter.getCount(eventId).getCountVal());
    }

    @Test
    public void should_return_zero_for_non_existing() {
        assertEquals(0, counter.getCount(UUID.randomUUID().toString()).getCountVal());
    }


    @Test
    public void should_handle_concurrency() throws Exception {
        final int threads = 12;
        final int eventCount = 250_000;

        String prefix = generateEventIdPrefix();
        ExecutorService executor = load(counter, threads, eventCount, prefix);
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        counter.sync();
        Awaitility.await().atMost(counter.getSyncInterval().multipliedBy(10)).until(() ->
                !counter.isSyncInProgress());
        // All counters should have correct values
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount, counter.getCount(prefix + value).getCountVal()));
    }

    @Test
    public void resetLocalMap_under_load() throws Exception {
        final int threads = 12;
        final int eventCount = 250_000; //250 times more than HazelcastCounterManager
        String prefix = generateEventIdPrefix();
        ExecutorService executor = load(counter, threads, eventCount, prefix);
        counter.resetLocalMap();
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        counter.sync();
        Awaitility.await().atMost(counter.getSyncInterval().multipliedBy(10)).until(() ->
                !counter.isSyncInProgress());
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount, counter.getCount(prefix + value).getCountVal()));
    }
}
