package org.berk.distributedcounter.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Response with id of the counter and its value
 */
public class Count implements Comparable<Count>{

    private final String id;
    private final Long count;

    @JsonCreator
    public Count(@JsonProperty("id") String id,
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
    public int compareTo(Count o) {
        return this.count.compareTo(o.count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Count that = (Count) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, count);
    }
}
