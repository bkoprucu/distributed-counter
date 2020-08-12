package org.berk.distributedcounter.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EventCountTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void shouldSerialize() throws Exception {
        Assert.assertEquals("{\"id\":\"Test Id\",\"count\":123}", MAPPER.writeValueAsString(new EventCount("Test Id", 123L)));
    }

    @Test
    public void shouldDeserialize() throws Exception {
        Assert.assertEquals(new EventCount("Test Id", 123L), MAPPER.readValue("{\"id\":\"Test Id\",\"count\":123}", EventCount.class));
    }
}