package org.berk.distributedcounter.testapp;

import org.berk.distributedcounter.client.DistributedCounterWebfluxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Applis load to counter using {@link DistributedCounterWebfluxClient}
 */
public class DistributedCounterPerformanceTester {
    private static final Logger log = LoggerFactory.getLogger(DistributedCounterPerformanceTester.class);

    private final DistributedCounterWebfluxClient client;

    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(30);


    private static final TestArgs WARMUP_PARAMS = new TestArgs(null,
                                                               4,
                                                               500,
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
    public PerformanceStats applyLoad(int numberOfCounters, int incrementBy, long speedLimit, Duration timeOut, boolean verify, boolean keepCounters)  {
        String countIdPrefix = generateRandomCountId();
        final int expectedRequestCount = numberOfCounters * incrementBy;

        AtomicInteger errorCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        Duration requestDelay = Duration.ofNanos(1_000_000_000L / speedLimit);

        log.info("About to send " + expectedRequestCount +  " requests to "+ client.getBaseUrl() + "/count, " +
                         "Performance ceiling: " + speedLimit + " req/s, Will increment "
                         + numberOfCounters + " counters prefixed with '" + countIdPrefix + "' by " + incrementBy);
        Instant startTime = Instant.now();
        try {
            Flux.interval(requestDelay)
                    .take(expectedRequestCount)
                    .map(i -> countIdPrefix + '_' + (i % numberOfCounters))
                    .onBackpressureBuffer(expectedRequestCount)
                    .flatMap(client::increment)
                    .doOnError(throwable -> {
                        log.error(throwable.getMessage());
                        errorCount.incrementAndGet(); })
                    .doOnNext(aBoolean -> successCount.incrementAndGet())
                    .blockLast(timeOut);

            log.info("Completed processing {} requests", expectedRequestCount);
            try {
                // Validate counts
                if (verify) {
                    log.info("Verifying data");
                    if (errorCount.get() > 0) {
                        log.error("{} requests not processed successfully. Skipping verification", errorCount.get());
                    } else {
                        Flux.interval(Duration.ofMillis(200))
                                .flatMap(i -> client.getCounters()
                                                      .filter(count -> count.getId().startsWith(countIdPrefix)
                                                                               && count.getCountVal().equals((long) incrementBy)).count())
                                .takeUntil(count -> count == numberOfCounters)
                                .blockFirst(VERIFY_TIMEOUT);
                        log.info("Verification completed");
                    }
                }
            } catch (Exception e) {
                log.error("Verification failed", e);
            }
            return new PerformanceStats(expectedRequestCount, successCount.get(), errorCount.get(), Duration.between(startTime, Instant.now()));
        } finally {
            if (!keepCounters) {
                log.info("Removing generated counters having prefix {}, ", countIdPrefix);
                try {
                    client.getCounters()
                            .filter(count -> count.getId().startsWith(countIdPrefix))
                            .flatMap(count -> client.removeCounter(count.getId()))
                            .blockLast(Duration.ofSeconds(5));
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
    public void warmup() {
        log.info("Sending warmup requests");
        log.info("Warmup stats: {}", applyLoad(WARMUP_PARAMS.getCounters(),
                                               WARMUP_PARAMS.getIncrementBy(),
                                               WARMUP_PARAMS.getSpeedLimit(),
                                               WARMUP_PARAMS.getTimeOut(),
                                               WARMUP_PARAMS.isVerifyCountsAfterTest(),
                                               WARMUP_PARAMS.isKeepCountersAfterTest()));

        // Wait for LocalCachingHazelcastCounter to finish with sync
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn("Interrupted on warmup() " + e.getMessage());
        }
    }

}



