package org.berk.distributedcounter.testapp;

import org.berk.distributedcounter.client.DistributedCounterWebfluxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Applis load to counter using {@link DistributedCounterWebfluxClient}
 */
public class DistributedCounterPerformanceTester {
    private static final Logger log = LoggerFactory.getLogger(DistributedCounterPerformanceTester.class);

    private final DistributedCounterWebfluxClient client;

    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(30);


    static TestArgs WARMUP_PARAMS = new TestArgs(null,
                                                 4, 500,
                                                 false,
                                                 true,
                                                 false,
                                                 Duration.ofSeconds(30),
                                                 200);

    public DistributedCounterPerformanceTester(DistributedCounterWebfluxClient client) {
        this.client = client;
    }

    private String generateRandomCountId() {
        return "performancetest_" + ThreadLocalRandom.current().nextLong();
    }

    /**
     * Apply load
     *
     * @param numberOfCounters Number of counters
     * @param incrementBy      How many times counters should be incremented
     * @param speedLimit       Max requests to send per second (defines the delay between increment operations)
*    * @param timeOut          Maximum time, after which test will be cancelled
     * @return Performance in req / sec
     */

    public PerformanceStats applyLoad(int numberOfCounters, int incrementBy, long speedLimit, Duration timeOut, boolean verify, boolean keepCounters) throws InterruptedException {
        String countIdPrefix = generateRandomCountId();
        final int expectedRequestCount = numberOfCounters * incrementBy;

        CountDownLatch completedLatch = new CountDownLatch(expectedRequestCount);
        AtomicInteger errorCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        Duration requestDelay = Duration.ofNanos(1_000_000_000L / speedLimit);

        log.info("About to send " + expectedRequestCount +  " requests to "+ client.getBaseUrl() + "/count, " +
                         "Performance ceiling: " + speedLimit + " req/s, Will increment "
                         + numberOfCounters + " counters prefixed with '" + countIdPrefix + "' by " + incrementBy);
        Instant startTime = Instant.now();
        try {
            Flux.interval(requestDelay)
                    .takeWhile(i -> i < expectedRequestCount)
                    .map(i -> countIdPrefix + '_' + (i % numberOfCounters))
                    .onBackpressureBuffer(expectedRequestCount)
                    .subscribe(
                            countId -> client.increment(countId).subscribe(
                            aBoolean -> successCount.incrementAndGet(),
                            error -> {
                                completedLatch.countDown();
                                log.error("Increment failed for countId: " + countId, error);
                                errorCount.incrementAndGet();
                            },
                            completedLatch::countDown));

            completedLatch.await(timeOut.getSeconds(), SECONDS);
            log.info("Completed processing {} requests", expectedRequestCount);
            try {
                // Validate counts
                if (verify) {
                    log.info("Verifying data");
                    if (errorCount.get() > 0) {
                        log.error("{} requests not processed successfully. Skipping verification", errorCount.get());
                    } else {
                        await().atMost(VERIFY_TIMEOUT).until(() -> numberOfCounters == client.getCounters()
                                                                                .filter(count -> count.getId().startsWith(countIdPrefix)
                                                                                                         && count.getCountVal().equals((long) incrementBy))
                                                                                .count().block());
                        log.info("Verification completed");
                    }
                }
            } catch (Exception e) {
                log.error("Verification failed", e);
            }
            return new PerformanceStats(expectedRequestCount, successCount.get(), errorCount.get(), Duration.between(startTime, Instant.now()));
        } finally {
            if(!keepCounters) {
                log.info("Removing generated counters having prefix {}, ", countIdPrefix);
                try {
                    List<Mono<Long>> listMono = client.getCounters()
                                                      .filter(count -> count.getId().startsWith(countIdPrefix))
                                                      .map(count -> client.removeCounter(count.getId()))
                                                      .collectList().block();
                    assert listMono != null;
                    Mono.when(listMono).block();
                    log.info("Removed items");
                } catch (Exception e) {
                    log.error("Error while removing the counters with prefix: " + countIdPrefix, e);
                }
            }
        }
    }


    /**
     * Apply predefined light load for warmup, before starting real test
     **/
    public void warmup() throws InterruptedException {
        log.info("Sending warmup requests");
        log.info("Warmup stats: {}", applyLoad(WARMUP_PARAMS.getCounters(),
                                               WARMUP_PARAMS.getIncrementBy(),
                                               WARMUP_PARAMS.getSpeedLimit(),
                                               WARMUP_PARAMS.getTimeOut(),
                                               WARMUP_PARAMS.isVerifyCountsAfterTest(),
                                               WARMUP_PARAMS.isKeepCountersAfterTest()));
        Thread.sleep(1000);
    }

}



