package org.bashar.distributedcounter.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import java.util.Objects;


/**
 * Response with id of the event and counter value
 * @param <T>  Type of the id of the event. Only String is supported by rest endpoint now
 */
public class EventCount<T> implements Comparable<EventCount>{

    private final T id;
    private final Long count;

    @JsonCreator
    public EventCount(@NotNull @JsonProperty("id") T id,
                      @NotNull @JsonProperty("count") Long count) {
        this.id = id;
        this.count = count;
    }

    public T getId() {
        return id;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public int compareTo(EventCount o) {
        return this.count.compareTo(o.count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCount<?> event = (EventCount<?>) o;
        return Objects.equals(id, event.id) &&
                Objects.equals(count, event.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, count);
    }
}
