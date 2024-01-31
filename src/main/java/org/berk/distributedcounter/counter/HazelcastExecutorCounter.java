package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.crdt.pncounter.PNCounter;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import org.berk.distributedcounter.rest.api.EventCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static java.util.Objects.requireNonNullElse;


/**
 * Counters based on {@link EntryProcessor}, to test EntryProcessor behavior
 *
 * It makes more sense to implement this using {@link PNCounter}
 *
 */
public class HazelcastExecutorCounter implements Counter {

    private static final Logger log = LoggerFactory.getLogger(HazelcastExecutorCounter.class);

    // Keeping counters
    private final IMap<String, Long> distributedMap;

    private final Deduplicator deduplicator;

    // Re-usable EntryProcessor for incrementing by one
    private final EntryProcessor<String, Long, Long> incrementByOneProcessor =
            entry -> requireNonNullElse(entry.setValue(entry.getValue() == null
                                    ? 1L
                                    : entry.getValue() + 1L), 0L);


    public HazelcastExecutorCounter(HazelcastInstance hazelcastInstance, Deduplicator deduplicator) {
        this.distributedMap = hazelcastInstance.getMap("CounterMap");
        this.deduplicator = deduplicator;
    }


    @Override
    public Mono<Long> incrementAsync(String eventId, Integer amount, String requestId) {
        // Atomically check for requestId for deduplication / idempotency
        return deduplicator.deduplicate(
                requestId,
                () -> Mono.fromCompletionStage(
                   distributedMap.submitToKey(eventId,
                                  amount == null || amount == 1 ? incrementByOneProcessor
                                              : entry -> requireNonNullElse(entry.setValue(entry.getValue() == null
                                                                                           ? amount
                                                                                           : entry.getValue() + amount), 0L))),
               () -> getCountAsync(eventId));
    }


    @Override
    public Mono<Long> getCountAsync(String eventId) {
        return Mono.fromCompletionStage(distributedMap.getAsync(eventId));
    }

    @Override
    public Mono<Integer> getSize() {
        return Mono.fromCallable(distributedMap::size)
                   .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<EventCount> getCounts() {
        return Flux.fromStream(() ->distributedMap
                .entrySet()
                .stream()
                .map(entry -> new EventCount(entry.getKey(), entry.getValue())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> remove(String eventId, String requestId) {
        return deduplicator.deduplicate(requestId,
                                        () -> Mono.fromRunnable(() -> distributedMap.delete(eventId)).then()
                                                  .subscribeOn(Schedulers.boundedElastic()),
                                        Mono::empty);
    }


    void clear() {
        log.warn("clear() called, removing all counters");
        distributedMap.clear();
    }

}
