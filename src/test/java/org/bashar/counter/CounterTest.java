package org.bashar.counter;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

public class CounterTest {

    @Test
    public void atomicTest() throws Exception {
        Assert.assertEquals(new AtomicLong(0L), new AtomicLong(0L));
    }
}
