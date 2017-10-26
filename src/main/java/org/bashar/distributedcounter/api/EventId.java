package org.bashar.distributedcounter.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * EventId to count the events
 * @param <T> Type of the id of the event. Currently, only String is supported by rest endpoint
 */
public class EventId<T> {

    private final T id;

    @JsonCreator
    public EventId(@NotNull @JsonProperty("id") T id) {
        this.id = id;
    }

    public T getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventId<?> event = (EventId<?>) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
