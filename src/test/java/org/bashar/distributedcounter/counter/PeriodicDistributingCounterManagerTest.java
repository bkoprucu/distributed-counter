package org.bashar.distributedcounter.counter;

import com.jayway.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class PeriodicDistributingCounterManagerTest extends HazelcastTest {

    private final PeriodicDistributingCounterManager<String> counterManager = new PeriodicDistributingCounterManager<>(hazelcastInstance);


    @Before
    public void setUp() throws Exception {
        counterManager.removeAll();
        assertEquals(0, counterManager.getSize());
        counterManager.init();
    }


    @Test
    public void shouldCountCorrectly() throws Exception {
        String id1 = "first";
        String id2 = "second";
        final long count = 100;
        assertEquals(0, counterManager.getCount(id1));
        assertEquals(0, counterManager.getCount(id2));
        IntStream.rangeClosed(1, (int)count).forEach(value ->  {
            counterManager.increment(id1);
            counterManager.increment(id2); });
        Awaitility.await().atMost(counterManager.getSyncInterval() * 3, MILLISECONDS).until(() ->
                counterManager.getSize() == 2);
        assertEquals(count, counterManager.getCount(id2));
        assertEquals(count, counterManager.getCount(id1));
        assertEquals(2, counterManager.getSize());
    }

    @Test
    public void shouldReturnZeroForNonExisting() throws Exception {
        assertEquals(0, counterManager.getCount("NonExisting"));
    }


    /**
     * This is also a load test.
     **/
    @Test
    public void shouldHandleMultipleThreads() throws Exception {
        final int threads = 100;
        final int eventCount = 1_500_000;
        ExecutorService executor = load(counterManager, threads, eventCount);
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        counterManager.sync();
        Awaitility.await().atMost(counterManager.getSyncInterval() * 10, MILLISECONDS).until(() ->
                !counterManager.isSyncInProgress());

        // All counters should have correct values
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount+value, counterManager.getCount("id" + value)));

    }

    @Test
    public void shouldHandleResetLocalMapUnderHeavyLoad() throws Exception {
        final int threads = 20;
        final int eventCount = 100_000; //250 times more than HazelcastCounterManager
        ExecutorService executor = load(counterManager, threads, eventCount);
        counterManager.resetLocalMap();
        counterManager.resetLocalMap();
        executor.shutdown();
        executor.awaitTermination(5, SECONDS);
        counterManager.sync();

        Awaitility.await().atMost(counterManager.getSyncInterval() * 10, MILLISECONDS).until(() ->
                !counterManager.isSyncInProgress());
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount+value, counterManager.getCount("id" + value)));

    }

}