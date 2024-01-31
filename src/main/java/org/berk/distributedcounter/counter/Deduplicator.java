package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Provide idempotency / deduplication using Hazelcast IMap
 */
@Component
public final class Deduplicator {

    private static final Logger log = LoggerFactory.getLogger(Deduplicator.class);

    private final IMap<String, Boolean> requestIdMap;

    public static final String DEDUPLICATION_MAP_NAME = "RequestIdMap";

    public Deduplicator(HazelcastInstance hazelcastInstance) {
        this.requestIdMap = hazelcastInstance.getMap(DEDUPLICATION_MAP_NAME);
    }

    /**
     * Atomically checks for requestId for deduplication / idempotency
     * @param requestId  Processed requests with this id will be rejected with {@code recover}
     * @param supplier   What to execute for given {@code requestId}
     * @param onDuplicate    What to execute if given {@code requestId} is already processed
     * @return           Result of {@code supplier} or {@code recover}
     */
    public <T> T deduplicate(String requestId,
                             Supplier<T> supplier,
                             @Nullable Supplier<T> onDuplicate) {
        if (requestId == null || requestIdMap.putIfAbsent(requestId, true) == null) {
            return supplier.get();
        }
        log.info("Duplicate request with requestId: {}", requestId);
        return onDuplicate == null ? null : onDuplicate.get();
    }


    void reset() {
        requestIdMap.clear();
    }
}
