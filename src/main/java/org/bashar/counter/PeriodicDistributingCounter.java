package org.bashar.counter;

import com.hazelcast.core.HazelcastInstance;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/**
 * Counts the events on local ConcurrentHashMap using AtomicLong, and synchronizes to Hazelcast periodically.
 * This way it can perform better than HazelcastCounter, though the results will be delayed by SYNC_INTERVAL
 * @param <T>
 */
@Service
public class PeriodicDistributingCounter<T> extends HazelcastCounter<T> {

    private final Logger log = LoggerFactory.getLogger(PeriodicDistributingCounter.class);

    // Synchronize with Hazelcast every 500ms
    private static final long SYNC_INTERVAL = 500L;

    private final ConcurrentHashMap<T, AtomicLong> localCountMap;
    private final ScheduledExecutorService scheduledExecutor;

    private final Lock lock;

    // We need to disable local counting during shutdown
    /**
     *
     */
    private final AtomicBoolean isLocalCounterEnabled;

    @Inject
    public PeriodicDistributingCounter(HazelcastInstance hazelcastInstance) {
        super(hazelcastInstance);
        localCountMap = new ConcurrentHashMap<>();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        isLocalCounterEnabled = new AtomicBoolean(true);
        lock = new ReentrantLock();
    }

    @PostConstruct
    private void init() {
        scheduledExecutor.scheduleWithFixedDelay(this::sync,
                SYNC_INTERVAL, SYNC_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void beforeShutDown() {
        log.info("Shutting down. Disabling local counters");
        isLocalCounterEnabled.set(false);
        scheduledExecutor.shutdown();
        sync();
    }

    @Override
    public void increment(T eventId) {
        if(isLocalCounterEnabled.get()) {
            // If there is no counter in the map, put 1 and return
            final AtomicLong cnt = localCountMap.putIfAbsent(eventId, new AtomicLong(1L));
            if (cnt != null) {
                cnt.incrementAndGet();
            }
        } else {
            super.increment(eventId);
        }
    }

    public long getLocalCount(T eventId) {
        final AtomicLong cnt = localCountMap.get(eventId);
        return cnt == null ? 0L : cnt.get();
    }

    private void sync() {
        log.debug("sync() initiated");
        localCountMap.entrySet().stream().filter(entry -> entry.getValue().get() != 0).forEach(entry -> {
            final AtomicLong atomicLong = entry.getValue();
            final long count = atomicLong.get();
            hazelcastIncrementer.increment(entry.getKey(), count);
            atomicLong.addAndGet(-count);
        });
        log.debug("sync() completed");
    }



    /**
     * Local map may be filled with inactive entrie with 0 counts after some time.
     * Since atomic removal from concurrent hashmap cannot be applied to AtomicLong
     * We have to switch to counting on Hazelcast only mode, sync() and remove the items.
     * On heathy system, this should clear all the items in the map.
     * This should not be done very often
     **/
    public int resetLocalMap() {
        try {
            isLocalCounterEnabled.compareAndSet(true, false);
            sync();
            // Local map should only have zero counts
            final List<T> zeroKeys = localCountMap.entrySet().stream()
                    .filter(entry -> entry.getValue().get() == 0L)
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toList());
            // Normally, all items should be 0
            if(localCountMap.mappingCount() == zeroKeys.size()) {
                localCountMap.clear();
            } else { // If some items cannot be synced
                zeroKeys.forEach(localCountMap::remove);
            }
            return zeroKeys.size();
        } finally {
            isLocalCounterEnabled.compareAndSet(false, true);
        }
    }

}
