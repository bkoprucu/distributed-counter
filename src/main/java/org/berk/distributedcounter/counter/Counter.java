package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.rest.api.EventCount;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import jakarta.annotation.Nullable;


public interface Counter {

    default Mono<Long> incrementAsync(String eventId, @Nullable String requestId) {
        return incrementAsync(eventId, 1, requestId);
    }


    /**
     * Increment counter {@code eventId} by given {@code amount}
     * @param eventId    Counter to increment
     * @param requestId  Unique id for idempotency, can be null
     * @return Former value of the counter, or null if there wasn't a counter
     */
    Mono<Long> incrementAsync(String eventId, Integer amount, @Nullable String requestId);

    /**
     * Remove counter {@code eventId}
     * @param eventId    Counter to delete or reset
     * @param requestId  Unique id for idempotency, can be null
     */
    Mono<Void> remove(String eventId, String requestId);

    Mono<Long> getCountAsync(String eventId);

    Mono<Integer> getSize();

    Flux<EventCount> getCounts();
}
