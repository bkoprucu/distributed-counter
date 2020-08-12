package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.api.EventCount;
import org.jvnet.hk2.annotations.Contract;

import java.util.List;

/**
 * @param <T> Counter item type
 */
@Contract
public interface Counter<T> {

    void increment(T eventId);

    void increment(T eventId, long amount);

    long getCount(T eventId);

    /**
     * List the login counters
     * @param fromIndex First index. null or negative values will be considered as 0 (beginning)
     * @param itemCount How many items to list, null will
     * @return Map containing counters
     */
    List<EventCount> listCounters(Integer fromIndex, Integer itemCount);

    /**
     * Returns total number of counters
     */
    int getSize();
}
