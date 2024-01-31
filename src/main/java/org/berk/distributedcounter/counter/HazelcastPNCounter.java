package org.berk.distributedcounter.counter;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.crdt.pncounter.PNCounter;
import org.berk.distributedcounter.rest.api.EventCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;
import java.util.stream.Stream;


/** Counters based on {@link PNCounter}, to test PNCounter behavior  */
public class HazelcastPNCounter implements Counter {

    private static final Logger log = LoggerFactory.getLogger(HazelcastPNCounter.class);

    private final HazelcastInstance hazelcast;

    private final Deduplicator deduplicator;

    public HazelcastPNCounter(HazelcastInstance hazelcastInstance, Deduplicator deduplicator) {
        this.hazelcast = hazelcastInstance;
        this.deduplicator = deduplicator;
    }


    @Override
    public Mono<Long> incrementAsync(String eventId, Integer amount, String requestId) {
        long delta = Objects.requireNonNullElse(amount, 1);
        return deduplicator.deduplicate(requestId,
            () -> Mono.fromCallable(() -> hazelcast.getPNCounter(eventId).getAndAdd(delta)),
            () -> {
                log.warn("Duplicate increment request for eventId: {}, requestId: {}", eventId, requestId);
                return getCountAsync(eventId);
            });
    }


    @Override
    public Mono<Void> remove(String eventId, String requestId) {
        return deduplicator.deduplicate(requestId,
                () -> Mono.fromRunnable(() -> {
                        log.info("Removing counter: {} ", eventId);
                        hazelcast.getPNCounter(eventId).destroy();
                }).subscribeOn(Schedulers.boundedElastic()).then(),
                () -> Mono.fromRunnable(() -> log.info("Duplicate remove request for eventId: {}  requestId: {}", eventId, requestId))
                          .then());
    }


    @Override
    public Mono<Long> getCountAsync(String eventId) {
        return Mono.just(hazelcast.getPNCounter(eventId).get());
    }

    @Override
    public Mono<Integer> getSize() {
        return Mono.fromCallable(() -> (int)
                           hazelcast.getDistributedObjects()
                                    .stream()
                                    .filter(PNCounter.class::isInstance)
                                    .count())
                   .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<EventCount> getCounts() {
        return Flux.fromStream(getAllCounterObjects().map(pnCounter -> new EventCount(pnCounter.getName(), pnCounter.get())))
                   .subscribeOn(Schedulers.boundedElastic());
    }

    private Stream<PNCounter> getAllCounterObjects() {
        return hazelcast.getDistributedObjects()
                        .stream()
                        .filter(PNCounter.class::isInstance)
                        .map(dObj -> (PNCounter) dObj);
    }


    void clear() {
        log.warn("clear() called, removing all counters");
        getAllCounterObjects().forEach(DistributedObject::destroy);
    }
}
