package org.bashar.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class HazelcastCounter<T> implements Counter<T> {

    static final String DISTRIBUTED_MAP_NAME = HazelcastCounter.class.getSimpleName().concat("Map");

    protected final IMap<T, Long> distributedMap;
    protected final HazelcastIncrementer<T> hazelcastIncrementer;

    @Inject
    public HazelcastCounter(HazelcastInstance hazelcastInstance) {
        distributedMap = hazelcastInstance.getMap(DISTRIBUTED_MAP_NAME);
        hazelcastIncrementer = new HazelcastIncrementer<>(distributedMap);
    }

    @Override
    public void increment(T eventId) {
        hazelcastIncrementer.increment(eventId);
    }

    @Override
    public long get(T eventId) {
        return distributedMap.getOrDefault(eventId, 0L);
    }

}
