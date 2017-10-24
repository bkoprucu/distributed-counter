package org.berk.distributedcounter.counter;

public interface Counter {

    /**
     * Increment by 1
     * @return Former value of the counter, or null if there wasn't a counter
     */
    Long increment(String eventId);

    /**
     * Increment by given `amount`
     * @return Former value of the counter, or null if there wasn't a counter
     */
    Long increment(String eventId, int amount);


    Long getCount(String eventId);

}
