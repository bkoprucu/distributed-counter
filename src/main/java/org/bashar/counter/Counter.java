package org.bashar.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class Counter {
    private final static Logger logger = LoggerFactory.getLogger(Counter.class);

    final static String DISTRIBUTED_MAP_NAME = "CounterMap";

    private final ConcurrentHashMap<String, AtomicLong> localCountMap;
    private final IMap<String, Long> distributedMap;
    private final HazelcastInstance hazelcastInstance;

    public Counter(final HazelcastInstance hazelcastInstance) {
        localCountMap = new ConcurrentHashMap<>();
        this.hazelcastInstance = hazelcastInstance;
        distributedMap = hazelcastInstance.getMap(DISTRIBUTED_MAP_NAME);
    }

    public long increment(final String user) {
        final AtomicLong cnt = localCountMap.putIfAbsent(user, new AtomicLong(1L));
        // If there was no value, return 1, else increment existing value and return
        return cnt == null ? 1L : cnt.incrementAndGet();
    }

    public long getLocalCount(final String user) {
        final AtomicLong cnt = localCountMap.get(user);
        return cnt == null ? 0L : cnt.get();
    }

    public long getCount(final String user) {
        return distributedMap.getOrDefault(user, 0L);
    }


    public void distribute() {
        localCountMap.entrySet().stream().filter(entry -> entry.getValue().get() != 0).forEach(entry -> {
            final String user = entry.getKey();
            final AtomicLong atomicLong = entry.getValue();
            final long count = atomicLong.get();
            if(distributedMap.putIfAbsent(user, count) == null) {
                atomicLong.addAndGet(-count);
            } else {
                distributedMap.executeOnKey(user, new AbstractEntryProcessor() {
                    @Override
                    public Object process(Map.Entry remoteEntry) {
                        remoteEntry.setValue((Long) remoteEntry.getValue() + count);
                        return null;
                    }
                });
                atomicLong.addAndGet(-count);
            }
        });
        removeLocalEntriesWithZero();
    }

    public void removeLocalEntriesWithZero() {
        final AtomicLong zero = new AtomicLong(0L);
        final List<String> keysMaybeWithZero = localCountMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(zero))
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
        // Atomically check and remove only the ones with zero counts
        keysMaybeWithZero.forEach(key ->  localCountMap.remove(key, zero));
    }

}
