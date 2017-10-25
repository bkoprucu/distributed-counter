package org.bashar.counter;

import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.map.EntryProcessor;

import java.util.Map;

/**
 * Atomic increment operations on Hazelcast IMap
 * @param <T> Key of the Hazelcast IMap
 */
class HazelcastIncrementer<T> {
    private final IMap<T, Long> distributedMap;

    private final EntryProcessor<T, Long> singleIncrementer;

    static final Long ONE = 1L;

    HazelcastIncrementer(IMap<T, Long> distributedMap) {
        this.distributedMap = distributedMap;
        this.singleIncrementer = new AbstractEntryProcessor<T, Long>() {
            @Override
            public Object process(Map.Entry<T, Long> entry) {
                final Long cnt = entry.getValue() + 1L;
                entry.setValue(cnt);
                return cnt;
            }
        };
    }

    Long increment(T eventId) {
        if(distributedMap.putIfAbsent(eventId, ONE) == null) {
            return ONE;
        } else {
            return (Long) distributedMap.executeOnKey(eventId, singleIncrementer);
        }
    }

    Long increment(T eventId, long amount) {
        if(amount == 0L || eventId == null) {
            return 0L;
        }
        if(distributedMap.putIfAbsent(eventId, amount) == null) {
            return amount;
        } else {
            return (Long) distributedMap.executeOnKey(eventId, new AbstractEntryProcessor<T, Long>() {
                @Override
                public Object process(Map.Entry<T, Long> entry) {
                    return entry.setValue(entry.getValue() + amount);
                }
            });
        }
    }
}
