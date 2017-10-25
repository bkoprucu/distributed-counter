package org.berk.distributedcounter.rest.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


public class EventCount implements Comparable<EventCount> {

    private final String id;
    private final Long count;

    @JsonCreator
    public EventCount(@JsonProperty("id") String id,
                      @JsonProperty("count") Long count) {
        this.id = id;
        this.count = count;
    }

    public String getId() {
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
        EventCount that = (EventCount) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, count);
    }
}
