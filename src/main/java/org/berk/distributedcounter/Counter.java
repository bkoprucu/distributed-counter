package org.berk.distributedcounter;

import org.berk.distributedcounter.rest.api.EventCount;

import java.util.List;

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

    Long remove(String eventId);

    Long getCount(String eventId);

    long getSize();

    List<EventCount> getCounts();
}
