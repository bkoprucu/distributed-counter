package org.bashar.distributedcounter.counter;

import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.map.EntryProcessor;

import java.util.Map;

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

        this.singleIncrementer = new AbstractEntryProcessor<T, Long>() {
            @Override
            public Object process(Map.Entry<T, Long> entry) {
                return entry.setValue(entry.getValue() + 1L);
            }
        };
    }

    void increment(T counterId) {
        if(counterId == null) {
            throw new IllegalArgumentException();
        }
        if (distributedMap.putIfAbsent(counterId, 1L) != null) {
            distributedMap.executeOnKey(counterId, singleIncrementer);
        }
    }

    void increment(T counterId, long amount) {
        if (counterId == null) {
            throw new IllegalArgumentException();
        }
        if (amount > 0L && distributedMap.putIfAbsent(counterId, amount) != null) {
            distributedMap.executeOnKey(counterId, new AbstractEntryProcessor<T, Long>() {
                @Override
                public Object process(Map.Entry<T, Long> entry) {
                    return entry.setValue(entry.getValue() + amount);
                }
            });
        }
    }


}

