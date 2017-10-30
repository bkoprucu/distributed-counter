package org.bashar.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import org.bashar.distributedcounter.Preferences;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
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
 * This way it can perform better than HazelcastCounter, though the results will be delayed by syncInterval
 */
@Service
public class PeriodicDistributingCounterManager<T> extends HazelcastCounterManager<T> {

    private final Logger logger = LoggerFactory.getLogger(PeriodicDistributingCounterManager.class);

    // Delay between sync operations
    private final long syncInterval = Preferences.PERIODIC_COUNTER_SYNC_DELAY;

    private final ConcurrentHashMap<T, AtomicLong> localMap;
    private final ScheduledExecutorService scheduledExecutor;

    // Lock is only used by administrative methods: stop() and resetLocalMap()
    private final Lock lock;

    // Used for disabling local counting during shutdown and #resetLocalMap calls
    private final AtomicBoolean localCounterEnabled;

    // Sync status
    private final AtomicBoolean syncInProgress;

    @Inject
    public PeriodicDistributingCounterManager(HazelcastInstance hazelcastInstance) {
        super(hazelcastInstance);
        localMap = new ConcurrentHashMap<>();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        localCounterEnabled = new AtomicBoolean(true);
        syncInProgress = new AtomicBoolean(false);
        lock = new ReentrantLock();
    }

    @PostConstruct
    void init() {
        logger.info("Starting sync executor. Delay={} ms", syncInterval);
        scheduledExecutor.scheduleWithFixedDelay(this::sync,
                syncInterval, syncInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Prepares to shutdown by stopping usin g local counting, syncing the current counts to server,
     * and stopping sync scheduler
     */
    @PreDestroy
    void stop() {
        logger.info("stop() initiated");
        lock.lock();
        try {
            logger.info("Shutting down. Disabling local counters");
            localCounterEnabled.set(false);
            scheduledExecutor.shutdown();
            sync();
        } finally {
            lock.unlock();
        }
        logger.info("stop() complete");
    }

    @Override
    public void increment(T counterId) {
        if (localCounterEnabled.get()) {
            // If there is no counter in the map, put 1 and return
            final AtomicLong cnt = localMap.putIfAbsent(counterId, new AtomicLong(1L));
            if (cnt != null) {
                cnt.incrementAndGet();
            }
        } else {
            super.increment(counterId);
        }
    }


    public long getSyncInterval() {
        return syncInterval;
    }

    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }

    /**
     * Sends local event count to Hazelcast, without locking
     */
     void sync() {
        if(syncInProgress.compareAndSet(false, true)) {
            try {
                logger.debug("sync() initiated");
                localMap.entrySet().stream().filter(entry -> entry.getValue().get() > 0).forEach(entry -> {
                    final AtomicLong atomicLong = entry.getValue();
                    final long count = atomicLong.get();
                    hazelcastIncrementer.increment(entry.getKey(), count);
                    atomicLong.addAndGet(-count);
                });
                logger.debug("sync() completed");
            } finally {
                syncInProgress.compareAndSet(true, false);
            }
        } else {
            logger.info("sync() skipped - alreeady in progress");
        }
    }

    /**
     * Administrative method to clear local map from collected inactive items with 0 counts.
     * <p>
     * Since atomic removal from ConcurrentHashmap cannot be applied to AtomicLong,
     * we have to switch to counting on Hazelcast only mode, sync() and remove the items.
     *
     * This can be scheduled to execute one time per day or week
     *
     * @return Number of removed items from local map
     *
     **/
    public int resetLocalMap() {
        logger.info("resetLocalMap() called");
        if (lock.tryLock()) {
            try {
                logger.info("resetLocalMap() initiated");
                localCounterEnabled.compareAndSet(true, false);
                sync();
                // Local map should only have zero counts
                final List<T> zeroKeys = localMap.entrySet().stream()
                        .filter(entry -> entry.getValue().get() == 0L)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                // Normally, all items should be 0
                if (localMap.mappingCount() == zeroKeys.size()) {
                    localMap.clear();
                    logger.info("resetLocalMap() removed all items ({}) from local map", zeroKeys.size());
                } else { // If some items cannot be synced
                    zeroKeys.forEach(localMap::remove);
                    logger.warn("resetLocalMap() removed {} items from local map, {} items remained.",
                            zeroKeys.size(), localMap.mappingCount() - zeroKeys.size());
                }
                return zeroKeys.size();
            } finally {
                lock.unlock();
                localCounterEnabled.compareAndSet(false, true);
                logger.info("resetLocalMap() completed");
            }
        } else {
            logger.info("resetLocalMap() is already in progress");
            return 0;
        }
    }

    /**
     * Administrative method to remove all Counters
     */
    @Override
    public void clear() {
        logger.warn("clear(): Removing all data!");
        distributedMap.clear();
        localMap.clear();
    }

}
