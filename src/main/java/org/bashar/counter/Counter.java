package org.bashar.counter;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface Counter<T> {
    void increment(T eventId);
    long get(T eventId);
}
