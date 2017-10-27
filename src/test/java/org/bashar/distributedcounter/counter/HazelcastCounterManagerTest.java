package org.bashar.distributedcounter.counter;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class HazelcastCounterManagerTest extends HazelcastTest {
    protected final HazelcastCounterManager<String> counterManager =
            new HazelcastCounterManager<>(hazelcastInstance);

    @Before
    public void setUp() throws Exception {
        counterManager.removeAll();
        assertEquals(0, counterManager.getSize());
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
        assertEquals(count, counterManager.getCount(id2));
        assertEquals(count, counterManager.getCount(id1));
        assertEquals(2, counterManager.getSize());
    }

    @Test
    public void shouldReturnZeroForNonExisting() throws Exception {
        assertEquals(0, counterManager.getCount("NonExisting"));
    }

    @Test
    public void shouldHandleMultipleThreads() throws Exception {
        final int threads = 100;
        final int eventCount = 6000;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for(int t=0; t < threads; t++ ){
            final int count = eventCount + t;
            final String id = "id" + t;
            executor.submit(() -> {
                for(int i=0; i < count; i++) {
                    counterManager.increment(id);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        // Counters should have correct values
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount+value, counterManager.getCount("id"+value)));

    }



}
