package org.berk.distributedcounter.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import org.berk.distributedcounter.Counter;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.util.Map;

public class HazelcastCounter implements Counter {

    private final IMap<String, Long> distributedMap;

    public static final String DEFAULT_DISTRIBUTED_MAP_NAME = HazelcastCounter.class.getSimpleName().concat("Map");

    @Inject
    public HazelcastCounter(HazelcastInstance hazelcastInstance) {
        this.distributedMap = hazelcastInstance.getMap(DEFAULT_DISTRIBUTED_MAP_NAME);
    }


    @Override
    public Long increment(String eventId) {
        return increment(eventId, 1);
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

}
