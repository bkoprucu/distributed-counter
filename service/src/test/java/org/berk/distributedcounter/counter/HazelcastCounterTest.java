package org.berk.distributedcounter.counter;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class HazelcastCounterTest extends HazelcastTest {
    protected final HazelcastCounter<String> counterManager =
            new HazelcastCounter<>(hazelcastInstance);

    @Test
    public void shouldCount() {
        String eventId = UUID.randomUUID().toString();
        int count = 100;
        IntStream.range(0, count).forEach(value ->  counterManager.increment(eventId));
        assertEquals(count, counterManager.getCount(eventId));
    }

    @Test
    public void shouldReturnZeroForNonExisting() {
        assertEquals(0, counterManager.getCount(UUID.randomUUID().toString()));
    }

    @Test
    public void shouldHandleMultipleThreads() throws Exception {
        final int threads = 24;
        final int eventCount = 3000;
        ExecutorService executor = load(counterManager, threads, eventCount, "counter-");
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        //  should have correct values
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount, counterManager.getCount("counter-" + value)));
    }

    @Test
    public void shouldGetSize() {
        int before = counterManager.getSize();

        IntStream.range(0, 10).forEach(value -> counterManager.increment(UUID.randomUUID().toString()));
        assertEquals(10, counterManager.getSize() - before);
    }

    // @Test
    public void shouldClear() {
        counterManager.increment(UUID.randomUUID().toString());
        counterManager.clear();
        assertEquals(0, counterManager.getSize());
    }

}
