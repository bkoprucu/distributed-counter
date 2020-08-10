package org.berk.distributedcounter.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * EventId to count the events
 */
public class EventId {

    private final String id;

    @JsonCreator
    public EventId(@NotNull @JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventId eventId = (EventId) o;
        return Objects.equals(id, eventId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
