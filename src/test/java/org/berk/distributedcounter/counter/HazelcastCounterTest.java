package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import org.berk.distributedcounter.rest.api.EventCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class HazelcastCounterTest {

    private final HazelcastInstance hazelcastInstance =
            HazelcastInstanceFactory.getOrCreateHazelcastInstance(new HazelcastConfigBuilder("testCluster")
                                                                          .withMulticastDiscovery()
                                                                          .getConfig());

    private final HazelcastCounter counter = new HazelcastCounter(hazelcastInstance,
                                                                  new HazelcastCounterProperties("testCluster", 10));

    private final String eventId = "testEventId";
    private final String nonExistingEventId = "nonExistingEventId";

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException {
        counter.clear();

        assertNull(counter.getCountAsync(eventId).block());
        assertNull(counter.getCountAsync(nonExistingEventId).block());
    }


    @Test
    public void shouldReturnNullForNonExisting() throws ExecutionException, InterruptedException {
        assertNull(counter.getCountAsync(nonExistingEventId).block());
    }

    @Test
    public void shouldIncrementByOne() throws ExecutionException, InterruptedException {
        long count = 10L;
        LongStream.range(0, count).forEach(value -> counter.incrementAsync(eventId, null));
        assertEquals(count, counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldIncrementByOneWhenAmountIsNull() throws ExecutionException, InterruptedException {
        counter.incrementAsync(eventId, null, null);
        assertEquals(1L, counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldIncrementByGivenAmount() throws ExecutionException, InterruptedException {
        long count = 10L;
        int amount = 2;
        LongStream.range(0, count).forEach(value -> counter.incrementAsync(eventId, amount, null));
        assertEquals(count * amount, counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldDeduplicateIncermentForSameRequestId() throws ExecutionException, InterruptedException {
        String requestId = UUID.randomUUID().toString();
        Long count = Flux.fromStream(
                                 IntStream.range(0, 5)
                                          .mapToObj(value -> counter.incrementAsync(eventId, requestId)))
                         .concatMap(longMono -> longMono)
                         .reduce((l1, l2) -> l2)
                         .block();
        assertEquals(1, count);
    }


    @Test
    public void shouldDeduplicateIncrementByGivenAmountAndSameRequestId()  {
        final int amount = 3;
        String requestId = UUID.randomUUID().toString();
        Long count = Flux.fromStream(
                                 IntStream.range(0, 5)
                                          .mapToObj(value -> counter.incrementAsync(eventId, amount, requestId)))
                         .concatMap(longMono -> longMono)
                         .reduce((l1, l2) -> l2)
                         .block();
        assertEquals(amount, count);
    }

    @Test
    public void shouldReturnSize() {
        assertEquals(0, counter.getSize().block());
        counter.incrementAsync("one", null);
        counter.incrementAsync("two", null);
        assertEquals(2, counter.getSize().block());
        counter.remove("one", null);
        assertEquals(1, counter.getSize().block());
    }

    @Test
    public void shouldListCounters() {
        List<EventCount> counts = Arrays.asList(new EventCount("one", 1L), new EventCount("two", 2L));
        counter.incrementAsync("one", null).block();
        counter.incrementAsync("two", 2, null).block();
        assertEquals(counts, counter.getCounts().collectList().block());
    }

    @Test
    public void shouldRemove() throws ExecutionException, InterruptedException {
        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, null);
        assertNull(counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldRemoveOnceForSameRequestId() throws ExecutionException, InterruptedException {
        String requestId = UUID.randomUUID().toString();
        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, requestId);
        assertNull(counter.getCountAsync(eventId).block());

        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, requestId); // Duplicate request, should be ignored
        assertEquals(5L, counter.getCountAsync(eventId).block());
    }

}