package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.api.EventCount;
import org.junit.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

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


    @Test
    public void should_list_counters() {
        counterManager.clear();
        Assumptions.assumeTrue(counterManager.getSize() == 0);

        int counterSize = 10;
        List<EventCount> expectedEventCounts = IntStream.range(0, counterSize).mapToObj(value ->  {
            counterManager.increment("counter_" + value);
            return new EventCount("counter_" + value, 1L);
        }).collect(Collectors.toList());

        List<EventCount> eventCounts = counterManager.listCounters(null, null);

        assertTrue(expectedEventCounts.containsAll(eventCounts));
        assertEquals(counterSize, eventCounts.size());

        // Test pagination
        List<EventCount> eventCountsPage1 = counterManager.listCounters(null, 4);
        List<EventCount> eventCountsPage2 = counterManager.listCounters(4, 4);
        List<EventCount> eventCountsPage3 = counterManager.listCounters(8, 4);
        assertEquals(4, eventCountsPage1.size());
        assertEquals(4, eventCountsPage2.size());
        assertEquals(2, eventCountsPage3.size());
        List<EventCount> allPages = new ArrayList<>(eventCountsPage1);
        allPages.addAll(eventCountsPage2);
        allPages.addAll(eventCountsPage3);
        assertTrue(expectedEventCounts.containsAll(allPages));
        assertEquals(counterSize, allPages.size());

    }

    @Test
    public void shouldClear() {
        counterManager.increment(UUID.randomUUID().toString());
        assertNotEquals(0, counterManager.getSize());
        counterManager.clear();
        assertEquals(0, counterManager.getSize());
    }

}
