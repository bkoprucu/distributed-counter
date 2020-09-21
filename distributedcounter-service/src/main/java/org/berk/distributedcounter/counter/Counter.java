package org.berk.distributedcounter.counter;

import org.berk.distributedcounter.api.Count;
import org.springframework.lang.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface Counter {

    default CompletableFuture<Boolean> incrementAsync(String counterId) {
        return incrementAsync(counterId, null);
    }

    /**
     * Increment the counter by {@code amount}, or by one if {@code amount} is null or less than 1}
     * @return {@code true}, if a new counter has been created, {@code false}, if existing counter has been incremented
     */
    CompletableFuture<Boolean> incrementAsync(String counterId, @Nullable Long amount);

    /**
     * @return Value of the counter, {@code null} if no counter has been found
     */
    CompletableFuture<Long> getCountAsync(String counterId);


    /**
     * Administrative method for removing / resetting  a counter
     * @return Value of the removed counter
     */
    CompletableFuture<Long> removeAsync(String counterId);


    /**
     * List counters
     * @param fromIndex   Index of first item, null or zero for first item
     * @param itemCount   Maximum number of items to list, or null, to list all
     * @return Map containing counters
     */
    Stream<Count> listCounters(@Nullable Integer fromIndex, @Nullable Integer itemCount);

    /**
     * @return Total number of counters
     */
    int getSize();
}
