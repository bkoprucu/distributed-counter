package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.rest.api.EventCount;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface Counter {

    /**
     * Increment counter {@code eventId} by one
     * @param eventId    Counter to increment
     * @param requestId  Unique id for idempotency, can be null
     * @return Former value of the counter, or null if there wasn't a counter
     */
    Mono<Long> incrementAsync(String eventId, String requestId);


    /**
     * Increment counter {@code eventId} by given {@code amount}
     * @param eventId    Counter to increment
     * @param amount     Increment by how much, can be null, defaults to 1
     * @param requestId  Unique id for idempotency, can be null
     * @return Former value of the counter, or null if there wasn't a counter
     */
    Mono<Long> incrementAsync(String eventId, @Nullable Integer amount, @Nullable String requestId);

    /**
     * Remove counter {@code eventId}
     * @param eventId    Counter to delete or reset
     * @param requestId  Unique id for idempotency, can be null
     */
    void remove(String eventId, String requestId);

    Mono<Long> getCountAsync(String eventId);

    Mono<Integer> getSize();

    Flux<EventCount> getCounts();
}
