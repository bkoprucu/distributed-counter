package org.berk.distributedcounter.client;

import org.berk.distributedcounter.api.Count;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


public class DistributedCounterClientTest {
    private static final Logger log = LoggerFactory.getLogger(DistributedCounterClientTest.class);
    private static final DistributedCounterWebfluxClient client = new DistributedCounterWebfluxClient("http://localhost:8080");

    private String generateCountId() {
        return "test_" + System.nanoTime();
    }


    @Test
    void should_handle_empty_result() {
        assertNull(client.getCount(generateCountId()).block());
    }

    @Test
    public void should_increment_and_get() {
        long count = 10;
        String countId = generateCountId();
        log.info(() -> "Incrementing counter: " + countId);
        List<Mono<Boolean>> monos = LongStream.range(0, count).mapToObj(i -> client.increment(countId)).collect(Collectors.toList());
        Mono.when(monos).block();
        System.out.println(
                client.getCount(countId).block()
        );
        //await().atMost(2, SECONDS).until(() -> Objects.equals(count, client.getCount(countId).block()));
    }


    @Test
    void should_get_list_size() {
        Integer listSize = client.getListSize().block();
        assertNotNull(listSize);
        client.increment(generateCountId()).block();
        await().atMost(ONE_SECOND).until(() -> client.getListSize().block() == listSize + 1);
    }


    public Long load(int numberOfCounters, int incrementBy, Duration requestDelay, Duration timeOut) throws InterruptedException {
        String countIdPrefix = generateCountId();
        final int expectedRequestCount = numberOfCounters * incrementBy;
        CountDownLatch completedLatch = new CountDownLatch(expectedRequestCount);
        AtomicInteger errorCount = new AtomicInteger();
        log.info(() ->"Incrementing " + numberOfCounters + " counters having prefix '" + countIdPrefix + "' by " + incrementBy);

        Instant startTime = Instant.now();
        Flux.fromStream(IntStream.range(0, incrementBy)
                .mapToObj(i -> IntStream.rangeClosed(1, numberOfCounters).mapToObj(j -> countIdPrefix + "_" + j))
                .flatMap(s -> s))
                .delayElements(requestDelay)
                .subscribe(countId -> client.increment(countId).subscribe(
                        null,
                        error -> { completedLatch.countDown();
                            log.error(error, () -> "Increment failed for countId: " + countId);
                            errorCount.incrementAndGet();},
                        completedLatch::countDown));

        completedLatch.await(3, MINUTES);
        long processingDuration = Duration.between(startTime, Instant.now()).toMillis();
        long reqPerSec = expectedRequestCount * 1000 /  processingDuration;
        log.info(() ->"Counting took " + processingDuration + " ms.(" + reqPerSec + " req./sec)");

        await().atMost(30, SECONDS).until(() ->
                numberOfCounters == client.getCounters()
                        .filter(count -> count.getId().startsWith(countIdPrefix) && count.getCountVal().equals((long) incrementBy))
                        .collectList().block().size()
        );
        log.info(() -> "Verification/sync  completed");

        return reqPerSec;
    }

    @Test
    void handle_load() throws InterruptedException {
        load(3, 1000, Duration.ofNanos(1000), Duration.ofMinutes(3));
        load(3, 10000, Duration.ofNanos(250), Duration.ofMinutes(3));
        load(300, 300, Duration.ofNanos(200), Duration.ofMinutes(3));
    }

    @Test
    public void should_get_list()  {
        List<Count> collect = client.getCounters().collect(Collectors.toList()).block();

    }


}
