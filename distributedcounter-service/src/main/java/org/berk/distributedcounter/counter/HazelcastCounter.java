package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.berk.distributedcounter.api.EventCount;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Counters on distributed Hazelcast Map
 */
@Service
public class HazelcastCounter<T> implements Counter<T> {

    private final Logger log = LoggerFactory.getLogger(HazelcastCounter.class);

    public static final String DISTRIBUTED_MAP_NAME = HazelcastCounter.class.getSimpleName().concat("Map");
    public static final int MAX_ITEMS_PER_PAGE = 100_000;


    protected final IMap<T, Long> distributedMap;
    protected final HazelcastIncrementer<T> hazelcastIncrementer;

    @Inject
    public HazelcastCounter(HazelcastInstance hazelcastInstance) {
        distributedMap = hazelcastInstance.getMap(DISTRIBUTED_MAP_NAME);
        hazelcastIncrementer = new HazelcastIncrementer<>(distributedMap);
    }

    @Override
    public void increment(T counterId) {
        hazelcastIncrementer.increment(counterId);
    }

    @Override
    public void increment(T counterId, long amount) {
        hazelcastIncrementer.increment(counterId, amount);
    }

    @Override
    public long getCount(T counterId) {
        return distributedMap.getOrDefault(counterId, 0L);
    }

    @Override
    public List<EventCount> listCounters(Integer fromIndex, Integer itemCount) {
        long skip = Optional.ofNullable(fromIndex).filter(fr -> fr > 0).orElse(0); // If from is negative or null, take 0
        long listSize = Optional.ofNullable(itemCount).orElse(MAX_ITEMS_PER_PAGE);

        return distributedMap.entrySet().stream()
                .skip(skip)
                .limit(listSize)
                .map(entry -> new EventCount(entry.getKey().toString(), entry.getValue()))
                .collect(Collectors.toList());
    }


    /** @inheritDoc */
    @Override
    public void removeCounter(T counterId) {
        log.info("Removing counter: {}", counterId);
        distributedMap.delete(counterId);
    }

    /**
     * Administrative method to remove all Counters
     */
    @Override
    public int getSize() {
        return distributedMap.size();
    }

    public void clear() {
        log.warn("clear() : Removing all data!");
        distributedMap.clear();
    }

}

































































































































































