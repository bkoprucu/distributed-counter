package org.berk.distributedcounter.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EventIdTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void shouldSerialize() throws Exception {
        Assert.assertEquals("{\"id\":\"Test Id\"}", MAPPER.writeValueAsString(new EventId("Test Id")));
    }

    @Test
    public void shouldDeserialize() throws Exception {
        Assert.assertEquals(new EventId("Test Id"), MAPPER.readValue("{\"id\":\"Test Id\"}", EventId.class));
    }
}