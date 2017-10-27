package org.bashar.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.bashar.distributedcounter.HazelcastConfig;
import org.bashar.distributedcounter.Preferences;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class HazelcastTest {

    protected final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(
            HazelcastConfig.getConfig("DistributedCounter_Test", 9300, Collections.singletonList("localhost")));

    protected ExecutorService load(CounterManager counterManager, int threads, int eventCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for(int t=0; t < threads; t++ ){
            final int count = eventCount + t;
            final String id = "id" + t;
            executor.submit(() -> {
                IntStream.range(0, count).forEach(value -> counterManager.increment(id));
            });
        }
        return executor;
    }
}
