package org.berk.distributedcounter.counter;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class PeriodicDistributingCounterTest extends HazelcastTest {

    private final PeriodicDistributingCounter<String> counterManager = new PeriodicDistributingCounter<>(hazelcastInstance);


    @Before
    public void setUp()  {
        counterManager.init();
    }

    @Test
    public void shouldCount()  {
        String eventId = UUID.randomUUID().toString();
        int count = 100;
        IntStream.range(0, count).forEach(value ->  counterManager.increment(eventId));
        Awaitility.await().atMost(counterManager.getSyncInterval() * 3, MILLISECONDS)
                .until(() -> !counterManager.isSyncInProgress() && counterManager.getCount(eventId) > 0);
        assertEquals(count, counterManager.getCount(eventId));
    }

    @Test
    public void shouldReturnZeroForNonExisting() {
        assertEquals(0, counterManager.getCount(UUID.randomUUID().toString()));
    }

    /**
     * This is also a load test.
     **/
    @Test
    public void shouldHandleMultipleThreads() throws Exception {
        final int threads = 12;
        final int eventCount = 250_0000;

        String prefix = generateEventIdPrefix();
        ExecutorService executor = load(counterManager, threads, eventCount, prefix);
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        counterManager.sync();
        Awaitility.await().atMost(counterManager.getSyncInterval() * 10, MILLISECONDS).until(() ->
                !counterManager.isSyncInProgress());
        // All counters should have correct values
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount, counterManager.getCount(prefix + value)));
    }

    @Test
    public void shouldHandleResetLocalMapUnderHeavyLoad() throws Exception {
        final int threads = 12;
        final int eventCount = 250_000; //250 times more than HazelcastCounterManager
        String prefix = generateEventIdPrefix();
        ExecutorService executor = load(counterManager, threads, eventCount, prefix);
        counterManager.resetLocalMap();
        counterManager.resetLocalMap();
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        counterManager.sync();
        Awaitility.await().atMost(counterManager.getSyncInterval() * 10, MILLISECONDS).until(() ->
                !counterManager.isSyncInProgress());
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount, counterManager.getCount(prefix + value)));
    }

}