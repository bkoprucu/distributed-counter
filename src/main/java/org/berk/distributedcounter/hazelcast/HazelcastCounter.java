package org.berk.distributedcounter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.map.EntryProcessor;
import org.berk.distributedcounter.Counter;
import org.berk.distributedcounter.rest.api.EventCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HazelcastCounter implements Counter {

    private static final Logger log = LoggerFactory.getLogger(HazelcastCounter.class);

    private final IMap<String, Long> distributedMap;

    public static final String DEFAULT_DISTRIBUTED_MAP_NAME = HazelcastCounter.class.getSimpleName().concat("Map");

    // Re-usable EntryProcessor for incrementing by one
    private final EntryProcessor<String, Long> singleIncrementProcessor = new AbstractEntryProcessor<String, Long>() {
        @Override
        public Object process(Map.Entry<String, Long> entry) {
            return entry.setValue(entry.getValue() == null ? 1L
                                                           : entry.getValue() + 1L);
        }
    };


    @Inject
    public HazelcastCounter(HazelcastInstance hazelcastInstance) {
        this.distributedMap = hazelcastInstance.getMap(DEFAULT_DISTRIBUTED_MAP_NAME);
    }


    @Override
    public Long increment(String eventId) {
        return (Long) distributedMap.executeOnKey(eventId, singleIncrementProcessor);
    }

    @Override
    public Long increment(String eventId, int amount) {
        if(amount == 0) {
            return getCount(eventId);
        }
        return (Long) distributedMap.executeOnKey(eventId, new AbstractEntryProcessor<String, Long>() {
            @Override
            public Object process(Map.Entry<String, Long> entry) {
                return entry.setValue(entry.getValue() == null ? amount
                                                               : entry.getValue() + amount);
            }
        });
    }


    @Override
    public Long getCount(String eventId) {
        return distributedMap.get(eventId);
    }

    @Override
    public long getSize() {
        return distributedMap.size();
    }

    @Override
    public List<EventCount> getCounts() {
        return distributedMap
                .entrySet()
                .stream()
                .map(entry -> new EventCount(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Long remove(String eventId) {
        Long removed = distributedMap.remove(eventId);
        log.info("Removed entry {} with value {}", eventId, removed);
        return removed;
    }

    void reset() {
        distributedMap.clear();
    }

}
