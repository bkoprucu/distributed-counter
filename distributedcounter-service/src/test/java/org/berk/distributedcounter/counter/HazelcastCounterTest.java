package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.api.Count;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class HazelcastCounterTest extends HazelcastTest {

    private final HazelcastCounter<String> counter = new HazelcastCounter<>(hazelcastInstance);

    @Test
    public void should_count() {
        String eventId = UUID.randomUUID().toString();
        int count = 100;
        IntStream.range(0, count).forEach(value ->  counter.increment(eventId));
        assertEquals(count, counter.getCount(eventId).getCountVal());
    }

    @Test
    public void should_return_zero_for_non_existing_counter() {
        assertEquals(0, counter.getCount(UUID.randomUUID().toString()).getCountVal());
    }

    @Test
    public void shouldHandleMultipleThreads() throws Exception {
        final int threads = 24;
        final int eventCount = 3000;
        ExecutorService executor = load(counter, threads, eventCount, "counter-");
        executor.shutdown();
        executor.awaitTermination(10, SECONDS);
        //  should have correct values
        IntStream.range(0, threads)
                .forEach(value -> assertEquals(eventCount, counter.getCount("counter-" + value).getCountVal()));
    }

    @Test
    public void should_get_size() {
        int before = counter.getSize();

        IntStream.range(0, 10).forEach(value -> counter.increment(UUID.randomUUID().toString()));
        assertEquals(10, counter.getSize() - before);
    }


    @Test
    public void should_list_counters() {
        counter.clear();
        Assumptions.assumeTrue(counter.getSize() == 0);

        int counterSize = 10;
        List<Count<String>> expectedCounts = IntStream.range(0, counterSize).mapToObj(value ->  {
            counter.increment("counter_" + value);
            return new Count<>("counter_" + value, 1L);
        }).collect(Collectors.toList());

        List<Count<String>> counts = counter.listCounters(null, null).collect(Collectors.toList());

        assertTrue(expectedCounts.containsAll(counts));
        assertEquals(counterSize, counts.size());

        // Test pagination
        Stream<Count<String>> countsPage1 = counter.listCounters(null, 4);
        Stream<Count<String>> countsPage2 = counter.listCounters(4, 4);
        Stream<Count<String>> countsPage3 = counter.listCounters(8, 4);
        List<Count<String>> allPages = Stream.concat(countsPage1, Stream.concat(countsPage2, countsPage3)).collect(Collectors.toList());
        assertTrue(expectedCounts.containsAll(allPages));
        assertEquals(counterSize, allPages.size());

    }

    @Test
    public void should_clear() {
        counter.increment(UUID.randomUUID().toString());
        assertNotEquals(0, counter.getSize());
        counter.clear();
        assertEquals(0, counter.getSize());
    }
}
