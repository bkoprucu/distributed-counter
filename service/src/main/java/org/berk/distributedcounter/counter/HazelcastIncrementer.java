package org.berk.distributedcounter.counter;

import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.map.EntryProcessor;

import java.util.Map;
import java.util.Optional;

/**
 * Atomic increment operations on Hazelcast IMap
 * Avoids locks by
 *
 * @param <T> Key of the Hazelcast IMap
 */
class HazelcastIncrementer<T> {
    private final IMap<T, Long> distributedMap;

    // EntryProcessor for incrementing one by one
    private final EntryProcessor<T, Long> singleIncrementer;

    HazelcastIncrementer(IMap<T, Long> distributedMap) {
        this.distributedMap = distributedMap;
        this.singleIncrementer = new AbstractEntryProcessor<>() {
            @Override
            public Object process(Map.Entry<T, Long> entry) {
                return Optional.ofNullable(entry.getValue())
                               .map(v -> entry.setValue(v + 1))
                               .orElseGet(() -> entry.setValue(1L));
            }
        };
    }

    void increment(T eventId) {
        distributedMap.executeOnKey(eventId, singleIncrementer);
    }

    void increment(T eventId, long amount) {
            distributedMap.executeOnKey(eventId, new AbstractEntryProcessor<T, Long>() {
                @Override
                public Object process(Map.Entry<T, Long> entry) {
                    return Optional.ofNullable(entry.getValue())
                            .map(v -> entry.setValue(v + amount))
                            .orElseGet(() -> entry.setValue(amount));
                }
            });
    }

}
