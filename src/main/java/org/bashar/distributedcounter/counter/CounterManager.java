package org.bashar.distributedcounter.counter;

import com.sun.istack.internal.Nullable;
import org.bashar.distributedcounter.api.EventCount;
import org.jvnet.hk2.annotations.Contract;

import java.util.List;

/**
 * @param <T> Counter item type
 */
@Contract
public interface CounterManager<T> {

    void increment(T counterId);

    long getCount(T eventId);

    /**
     * List the login counters
     * @param from First index. null will be considered as 0 (beginning)
     * @param to End index for paging. null to getCount all items
     * @return Map containing counters
     */
    List<EventCount<T>> listAllCounters(@Nullable Integer from , @Nullable Integer to);

    /**
     * Returns total number of counters
     */
    int getSize();
}
