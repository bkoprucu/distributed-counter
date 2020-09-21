package org.berk.distributedcounter.counter;


import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class LocalCachingHazelcastCounterTest extends HazelcastTest {

    private final LocalCachingHazelcastCounter counter = new LocalCachingHazelcastCounter(hazelcastInstance, Duration.ofMillis(500));


    @BeforeEach
    public void setUp()  {
        counter.init();
    }

    @Test
    public void increment()  {
        String countId = randomCountId();
        int count = 10;
        IntStream.range(0, count).forEach(value ->  counter.incrementAsync(countId).join());
        Awaitility.await().atMost(counter.getSyncInterval().multipliedBy(5))
                .until(() -> !counter.isSyncInProgress() && counter.getCountAsync(countId).join() != null);
        assertEquals(count, counter.getCountAsync(countId).join());
    }

    @Test
    public void getCountAsync_should_return_null_for_non_existing() {
        assertNull(counter.getCountAsync(randomCountId()).join());
    }


    @Test
    public void handle_concurrency_while_resetLocalMap_is_working() {
        final int eventCount = 100_000;
        String prefix = randomCountId();
        CompletableFuture<Void> loadFuture = load(counter, eventCount, prefix);
        counter.resetLocalMap();
        counter.sync();
        Awaitility.await().atMost(FIVE_SECONDS).until(() ->
                !counter.isSyncInProgress());
        loadFuture.join();
        assertTrue(loadFuture.isDone());
        IntStream.range(0, eventCount)
                .forEach(value -> assertEquals(1, counter.getCountAsync(prefix + value).join()));
    }
}
