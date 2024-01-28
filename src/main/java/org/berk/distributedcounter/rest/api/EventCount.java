package org.berk.distributedcounter.rest.api;

public record EventCount(String id, Long count) implements Comparable<EventCount> {

    @Override
    public int compareTo(EventCount o) {
        return this.count.compareTo(o.count);
    }

}
