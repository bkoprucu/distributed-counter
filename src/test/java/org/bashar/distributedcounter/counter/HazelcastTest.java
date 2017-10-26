package org.bashar.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.bashar.distributedcounter.HazelcastConfigFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HazelcastTest {
    public static final String HAZELCAST_INSTANCE_NAME = "DistributedCounter_Test";

    protected final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory
            .getOrCreateHazelcastInstance(HazelcastConfigFactory.hazelCastConfig(HAZELCAST_INSTANCE_NAME,"localhost"));

    protected ExecutorService load(CounterManager counterManager, int threads, int eventCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for(int t=0; t < threads; t++ ){
            final int count = eventCount + t;
            final String id = "id" + t;
            executor.submit(() -> {
                for(int i=0; i < count; i++) {
                    counterManager.increment(id);
                }
            });
        }
        return executor;
    }
}
