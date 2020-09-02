package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import org.berk.distributedcounter.rest.api.EventCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.TimeUnit.SECONDS;

public class HazelcastCounter implements Counter {

    private static final Logger log = LoggerFactory.getLogger(HazelcastCounter.class);

    private final HazelcastCounterProperties counterProperties;

    // Keeping counters
    private final IMap<String, Long> distributedMap;

    // For idempotence / deduplication
    final IMap<String, Boolean> requestIdMap;

    // Re-usable EntryProcessor for incrementing by one
    private final EntryProcessor<String, Long, Long> incrementByOneProcessor =
            entry -> entry.setValue(entry.getValue() == null
                                    ? 1L
                                    : entry.getValue() + 1L);


    public HazelcastCounter(HazelcastInstance hazelcastInstance, HazelcastCounterProperties counterProperties) {
        this.counterProperties = counterProperties;
        this.distributedMap = hazelcastInstance.getMap(getClass().getSimpleName().concat("Map"));
        this.requestIdMap = hazelcastInstance.getMap("RequestIdMap");
    }


    @Override
    public Mono<Long> incrementAsync(String eventId, String requestId) {
        return incrementAsync(eventId, 1, requestId);
    }


    @Override
    public Mono<Long> incrementAsync(String eventId, Integer amount, String requestId) {
        final int amnt = amount == null ? 1 : amount;
        if (amnt == 0) {
            return getCountAsync(eventId);
        }

        // Atomically check for requestId for deduplication / idempotency
        if (requestId == null || requestIdMap.putIfAbsent(requestId, true, counterProperties.getDeduplicationMapTimeOutSecs(), SECONDS) == null) {
            CompletionStage<Long> completionStage =
                distributedMap.submitToKey(eventId, amnt == 1
                        ? incrementByOneProcessor
                        : entry -> entry.setValue(entry.getValue() == null ? amnt
                                                                           : entry.getValue() + amnt));

            return Mono.fromCompletionStage(completionStage);
        }
        log.warn("Duplicate increment request for eventId: {}, requestId: {}", eventId, requestId);
        return getCountAsync(eventId);
    }


    @Override
    public Mono<Long> getCountAsync(String eventId) {
        return Mono.fromCompletionStage(distributedMap.getAsync(eventId));
    }

    @Override
    public Mono<Integer> getSize() {
        return Mono.fromCallable(distributedMap::size);
    }

    @Override
    public Flux<EventCount> getCounts() {
        return Flux.fromStream(() ->distributedMap
                .entrySet()
                .stream()
                .map(entry -> new EventCount(entry.getKey(), entry.getValue())));
    }

    @Override
    public void remove(String eventId, String requestId) {
        // Atomically check for requestId for deduplication / idempotency
        if (requestId == null || requestIdMap.putIfAbsent(requestId, true, counterProperties.getDeduplicationMapTimeOutSecs(), SECONDS) == null) {
            distributedMap.delete(eventId);
            log.info("Removed entry {}", eventId);
            return;
        }
        log.info("Duplicate remove request for eventId: {}.  with requestId: {}", eventId, requestId);
    }


    void clear() {
        log.info("clear() called, removing all counters");
        distributedMap.clear();
    }

}
