package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.api.Count;

import java.util.stream.Stream;

/**
 * @param <T> Counter item type
 */
public interface Counter {

    void increment(String counterId);

    void increment(String counterId, long amount);

    Count getCount(String counterId);

    /**
     * Administrative method for removing / resetting  a counter
     * */
    void removeCounter(String counterId);


    /**
     * List the login counters
     * @param fromIndex First index. null or negative values will be considered as 0 (beginning)
     * @param itemCount How many items to list, null will
     * @return Map containing counters
     */
    Stream<Count> listCounters(Integer fromIndex, Integer itemCount);

    /**
     * Returns total number of counters
     */
    int getSize();
}
