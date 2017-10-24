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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HazelcastCounter implements Counter {

    private static final Logger log = LoggerFactory.getLogger(HazelcastCounter.class);

    // Keeping counters
    private final IMap<String, Long> distributedMap;

    // For idempotence
    private final IMap<String, Boolean> requestIdMap;


    // Re-usable EntryProcessor for incrementing by one
    private final EntryProcessor<String, Long> incrementByOneProcessor = new AbstractEntryProcessor<String, Long>() {
        @Override
        public Object process(Map.Entry<String, Long> entry) {
            return entry.setValue(entry.getValue() == null ? 1L
                                                           : entry.getValue() + 1L);
        }
    };


    @Inject
    public HazelcastCounter(HazelcastInstance hazelcastInstance) {
        distributedMap = hazelcastInstance.getMap(getClass().getSimpleName().concat("Map"));
        requestIdMap = hazelcastInstance.getMap("RequestIdMap");
    }


    @Override
    public Long increment(String eventId, String requestId) {
        // Atomically check for requestId to prevent duplicate processing (idempotency)
        if(requestId == null || requestIdMap.putIfAbsent(requestId, true, 10, TimeUnit.MINUTES) == null) {
            return (Long) distributedMap.executeOnKey(eventId, incrementByOneProcessor);
        }
        log.warn("Duplicate increment request for eventId: {}, requestId: {}", eventId, requestId);
        return getCount(eventId);
    }


    @Override
    public Long increment(String eventId, int amount, String requestId) {
        if(amount == 0) {
            return getCount(eventId);
        }
        // Atomically check for requestId to prevent duplicate processing (idempotency)
        if(requestId == null || requestIdMap.putIfAbsent(requestId, true, 10, TimeUnit.MINUTES) == null) {
            return (Long) distributedMap.executeOnKey(eventId, new AbstractEntryProcessor<String, Long>() {
                @Override
                public Object process(Map.Entry<String, Long> entry) {
                    return entry.setValue(entry.getValue() == null ? amount
                                                                   : entry.getValue() + amount);
                }
            });
        }
        log.warn("Duplicate increment request for eventId: {}, requestId: {}", eventId, requestId);
        return getCount(eventId);
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
    public void remove(String eventId, String requestId) {
        if(requestId == null || requestIdMap.putIfAbsent(requestId, true, 10, TimeUnit.MINUTES) == null) {
            distributedMap.delete(eventId);
            log.info("Removed entry {}", eventId);
            return;
        }
        log.info("Duplicate remove request for eventId: {}.  with requestId: {}", eventId, requestId);
    }

    void clear() {
        log.info("clear() called, removing all counters");
        distributedMap.clear();
    }

}
