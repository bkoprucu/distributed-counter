package org.berk.distributedcounter.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Response with id of the counter and its value
 */
public class Count implements Comparable<Count>{

    private final String id;
    private final Long countVal;

    @JsonCreator
    public Count(@JsonProperty("id") String id,
                 @JsonProperty("countVal") Long countVal) {
        if(id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        this.id = id;
        this.countVal = countVal == null ? 0L : countVal;
    }

    public String getId() {
        return id;
    }

    public Long getCountVal() {
        return countVal;
    }

    @Override
    public int compareTo(Count o) {
        return this.countVal.compareTo(o.countVal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Count count1 = (Count) o;
        return id.equals(count1.id) &&
                countVal.equals(count1.countVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, countVal);
    }

    @Override
    public String toString() {
        return "Count{" +
                "id='" + id + '\'' +
                ", countVal=" + countVal +
                '}';
    }
}
