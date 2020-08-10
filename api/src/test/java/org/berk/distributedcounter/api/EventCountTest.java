package org.berk.distributedcounter.api;

import org.junit.Assert;
import org.junit.Test;

public class EventCountTest {


    @Test
    public void shouldSerialize() throws Exception {
        Assert.assertEquals("{\"id\":\"Test Id\",\"count\":123}", EventIdTest.MAPPER.writeValueAsString(new EventCount("Test Id", 123L)));
    }

    @Test
    public void shouldDeserialize() throws Exception {
        Assert.assertEquals(new EventCount("Test Id", 123L), EventIdTest.MAPPER.readValue("{\"id\":\"Test Id\",\"count\":123}", EventCount.class));
    }
}