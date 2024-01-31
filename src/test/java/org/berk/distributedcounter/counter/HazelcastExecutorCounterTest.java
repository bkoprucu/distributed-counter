package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.rest.api.EventCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class HazelcastExecutorCounterTest extends HazelcastTest {

    private final Deduplicator deduplicator = new Deduplicator(hazelcastInstance);
    private final HazelcastExecutorCounter counter = new HazelcastExecutorCounter(hazelcastInstance, deduplicator);

    private final String eventId = "testEventId";
    private final String nonExistingEventId = "nonExistingEventId";

    @BeforeEach
    public void setUp()  {
        counter.clear();
        deduplicator.reset();
        assertNull(counter.getCountAsync(eventId).block());
        assertNull(counter.getCountAsync(nonExistingEventId).block());
    }


    @Test
    public void shouldReturnNullForNonExisting()  {
        assertNull(counter.getCountAsync(nonExistingEventId).block());
    }

    @Test
    public void shouldIncrementByOne()  {
        assertEquals(0, counter.incrementAsync(eventId, null).block());
        assertEquals(1, counter.incrementAsync(eventId, null).block());
        assertEquals(2, counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldIncrementByOneWhenAmountIsNull() {
        assertEquals(0, counter.incrementAsync(eventId, null, null).block());
        assertEquals(1L, counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldIncrementByGivenAmount() {
        final int delta = 5;
        assertEquals(0, counter.incrementAsync(eventId, delta, null).block());
        assertEquals(5, counter.incrementAsync(eventId, delta, null).block());
        assertEquals(10, counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldDeduplicateIncermentForSameRequestId() {
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
    public void shouldDeduplicateIncrementByGivenAmountAndSameRequestId() {
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
        counter.remove("one", null).block();
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
    public void shouldRemove() {
        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, null).block();
        assertNull(counter.getCountAsync(eventId).block());
    }

    @Test
    public void shouldRemoveOnceForSameRequestId() {
        String requestId = UUID.randomUUID().toString();
        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, requestId).block();
        assertNull(counter.getCountAsync(eventId).block());

        counter.incrementAsync(eventId, 5, null).block();
        assertEquals(5L, counter.getCountAsync(eventId).block());
        counter.remove(eventId, requestId).block(); // Duplicate request, should be ignored
        assertEquals(5L, counter.getCountAsync(eventId).block());
    }

}