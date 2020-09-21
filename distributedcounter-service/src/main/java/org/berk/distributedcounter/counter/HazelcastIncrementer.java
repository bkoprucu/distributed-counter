package org.berk.distributedcounter.counter;

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Atomic increment operations on Hazelcast IMap
 * Avoids locks by using Hazelcast executor
 *
 * @param <T> Key of the Hazelcast IMap
 */
class HazelcastIncrementer<T> {
    private final IMap<T, Long> distributedMap;

    // EntryProcessor for incrementing by one
    private final EntryProcessor<T, Long, Long> singleIncrementer;

    HazelcastIncrementer(IMap<T, Long> distributedMap) {
        this.distributedMap = distributedMap;
        this.singleIncrementer = entry -> Optional.ofNullable(entry.getValue())
                .map(v -> entry.setValue(v + 1))
                .orElseGet(() -> entry.setValue(1L));
    }


    CompletableFuture<Long> increment(T eventId, Long amount) {
        if(amount == null || amount.equals(1L)) {
            return distributedMap.submitToKey(eventId, singleIncrementer)
                    .toCompletableFuture();
        }
        return distributedMap.submitToKey(eventId,
                entry -> Optional.ofNullable(entry.getValue())
                        .map(v -> entry.setValue(v + amount))
                        .orElseGet(() -> entry.setValue(amount)))
                .toCompletableFuture();
    }

}
