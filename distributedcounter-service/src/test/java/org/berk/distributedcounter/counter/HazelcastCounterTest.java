package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.api.Count;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class HazelcastCounterTest extends HazelcastTest {

    private final HazelcastCounter counter = new HazelcastCounter(hazelcastInstance);

    @Test
    public void increment() {
        String countId = randomCountId();
        int count = 100;
        IntStream.range(0, count).forEach(value ->  counter.incrementAsync(countId).join());
        assertEquals(count, counter.getCountAsync(countId).join());
    }

    @Test
    public void return_null_for_non_existing_counter() {
        assertNull(counter.getCountAsync(randomCountId()).join());
    }



    @Test
    public void handle_concurrency() throws Exception {
        final int eventCount = 50_000;
        String countIdPrefix = randomCountId();
        CompletableFuture<Void> loadFuture1 = load(counter, eventCount, countIdPrefix);
        CompletableFuture<Void> loadFuture2 = load(counter, eventCount, countIdPrefix);
        loadFuture1.join();
        loadFuture2.join();
        assertTrue(loadFuture1.isDone());
        assertTrue(loadFuture2.isDone());
        //  should have correct values
        IntStream.range(0, eventCount)
                .forEach(value -> assertEquals(2, counter.getCountAsync(countIdPrefix + value).join()));
    }

    @Test
    public void getSize() {
        int before = counter.getSize();

        IntStream.range(0, 10).forEach(value -> counter.incrementAsync(randomCountId()));
        assertEquals(10, counter.getSize() - before);
    }


    @Test
    public void listCounters() {
        counter.clear();
        Assumptions.assumeTrue(counter.getSize() == 0);

        int counterSize = 10;
        List<Count> expectedCounts = IntStream.range(0, counterSize).mapToObj(value ->  {
            counter.incrementAsync("counter_" + value);
            return new Count("counter_" + value, 1L);
        }).collect(Collectors.toList());

        List<Count> counts = counter.listCounters(null, null).collect(Collectors.toList());

        assertTrue(expectedCounts.containsAll(counts));
        assertEquals(counterSize, counts.size());

        // Test pagination
        Stream<Count> countsPage1 = counter.listCounters(null, 4);
        Stream<Count> countsPage2 = counter.listCounters(4, 4);
        Stream<Count> countsPage3 = counter.listCounters(8, 4);
        List<Count> allPages = Stream.concat(countsPage1, Stream.concat(countsPage2, countsPage3)).collect(Collectors.toList());
        assertTrue(expectedCounts.containsAll(allPages));
        assertEquals(counterSize, allPages.size());

    }

    @Test
    public void clear() {
        counter.incrementAsync(randomCountId());
        assertNotEquals(0, counter.getSize());
        counter.clear();
        assertEquals(0, counter.getSize());
    }
}
