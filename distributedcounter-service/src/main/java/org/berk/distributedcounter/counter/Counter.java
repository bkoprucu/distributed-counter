package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.api.Count;
import org.jvnet.hk2.annotations.Contract;

import java.util.List;

/**
 * @param <T> Counter item type
 */
@Contract
public interface Counter<T> {

    void increment(T counterId);

    void increment(T counterId, long amount);

    long getCount(T counterId);

    /**
     * Administrative method for removing / resetting  a counter
     * */
    void removeCounter(T counterId);


    /**
     * List the login counters
     * @param fromIndex First index. null or negative values will be considered as 0 (beginning)
     * @param itemCount How many items to list, null will
     * @return Map containing counters
     */
    List<Count> listCounters(Integer fromIndex, Integer itemCount);

    /**
     * Returns total number of counters
     */
    int getSize();

}
