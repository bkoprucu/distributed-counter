package org.berk.distributedcounter.client;

import org.berk.distributedcounter.api.Count;

import java.util.List;

public interface CounterClient {
    /** @return Value of the counter identified with counterId */
    long getCount(String counterId);

    /** Increment counter by one */
    void increment(String counterId);

    /** Increment counter by amount */
    void increment(String counterId, Long amount);


    /**
     * List counters
     * @param fromIndex  For pagination, index of the list to start from, can be null
     * @param itemCount  How many items to list, can be null to list max. items
     * @return List of {@link Count }
     */
    List<Count<String>> getCounters(Integer fromIndex, Integer itemCount);

    /**
     * @return Total number of counters
     */
    int getListSize();
}
