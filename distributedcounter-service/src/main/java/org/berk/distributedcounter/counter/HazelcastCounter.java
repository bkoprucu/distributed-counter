package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.berk.distributedcounter.api.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * Counters on distributed Hazelcast Map
 */
public class HazelcastCounter implements Counter {

    private final Logger log = LoggerFactory.getLogger(HazelcastCounter.class);

    public static final String MAP_NAME = HazelcastCounter.class.getSimpleName().concat("Map");
    public static final int MAX_ITEMS_PER_PAGE = 100_000;


    protected final IMap<String, Long> distributedMap;
    protected final HazelcastIncrementer<String> hazelcastIncrementer;

    public HazelcastCounter(HazelcastInstance hazelcastInstance) {
        distributedMap = hazelcastInstance.getMap(MAP_NAME);
        hazelcastIncrementer = new HazelcastIncrementer<>(distributedMap);
    }

    @Override
    public void increment(String counterId) {
        hazelcastIncrementer.increment(counterId);
    }

    @Override
    public void increment(String counterId, long amount) {
        hazelcastIncrementer.increment(counterId, amount);
    }

    @Override
    public Count getCount(String counterId) {
        return new Count(counterId, distributedMap.getOrDefault(counterId, 0L));
    }

    @Override
    public Stream<Count> listCounters(Integer fromIndex, Integer itemCount) {
        long skip = Optional.ofNullable(fromIndex).filter(fr -> fr > 0).orElse(0); // If from is negative or null, take 0
        long listSize = Optional.ofNullable(itemCount).orElse(MAX_ITEMS_PER_PAGE);

        return distributedMap.entrySet().stream()
                .skip(skip)
                .limit(listSize)
                .map(entry -> new Count(entry.getKey(), entry.getValue()));
    }


    /** @inheritDoc */
    @Override
    public void removeCounter(String counterId) {
        log.info("Removing counter: {}", counterId);
        distributedMap.delete(counterId);
    }

    /**
     * @return Item count, how many count
     */
    @Override
    public int getSize() {
        return distributedMap.size();
    }

    /**
     * Administrative method to remove all Counters
     */
    public void clear() {
        log.warn("clear() : Removing all data!");
        distributedMap.clear();
    }

}





























































































































































