package org.berk.distributedcounter;

import org.berk.distributedcounter.rest.api.EventCount;

import java.util.List;

public interface Counter {

    /**
     * Increment counter {@code eventId} by 1
     *
     * @param requestId Unique request id for idempotence, can be null
     * @return Former value of the counter, or null if there wasn't a counter
     */
    Long increment(String eventId, String requestId);


    /**
     * Increment counter {@code eventId} by given {@code amount}
     * @param requestId Unique request id for idempotence, can be null
     * @return Former value of the counter, or null if there wasn't a counter
     */
    Long increment(String eventId, int amount, String requestId);

    /**
     * Remove counter {@code eventId}
     * @param requestId Unique request id for idempotence, can be null
     */
    void remove(String eventId, String requestId);

    Long getCount(String eventId);

    long getSize();

    List<EventCount> getCounts();
}
