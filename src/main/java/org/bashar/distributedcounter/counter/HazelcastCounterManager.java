package org.bashar.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.bashar.distributedcounter.api.EventCount;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counters on distributed Hazelcast Map
 */
@Service
public class HazelcastCounterManager<T> implements CounterManager<T> {

    private final Logger log = LoggerFactory.getLogger(HazelcastCounterManager.class);

    public static final String DISTRIBUTED_MAP_NAME = HazelcastCounterManager.class.getSimpleName().concat("Map");

    protected final IMap<T, Long> distributedMap;
    protected final HazelcastIncrementer<T> hazelcastIncrementer;

    @Inject
    public HazelcastCounterManager(HazelcastInstance hazelcastInstance) {
        distributedMap = hazelcastInstance.getMap(DISTRIBUTED_MAP_NAME);
        hazelcastIncrementer = new HazelcastIncrementer<>(distributedMap);
    }

    @Override
    public void increment(T counterId) {
        hazelcastIncrementer.increment(counterId);
    }

    @Override
    public long getCount(T eventId) {
        return distributedMap.getOrDefault(eventId, 0L);
    }

    @Override
    public List<EventCount<T>> listAllCounters(Integer from, Integer to) {
        final AtomicInteger toAtomic = to == null ? null : new AtomicInteger(to);
        final LinkedList<EventCount<T>> result = new LinkedList<>();
        distributedMap.entrySet().stream()
                .sorted()
                .skip(from == null ? 0 : from)
                .forEachOrdered(entry -> {
                    if(toAtomic == null || toAtomic.getAndDecrement()>0) {
                        result.add(new EventCount<>(entry.getKey(), entry.getValue()));
                        // Ugly, but IMap refuses to work with Collectors.collect()
                    }
                });
        return result;
    }

    @Override
    public int getSize() {
        return distributedMap.size();
    }

    /**
     * Administrative method to remove all Counters
     * */
    void removeAll() {
        log.warn("removeAll() : Removing all data!");
        distributedMap.clear();
    }


}
