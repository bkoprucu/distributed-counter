package org.berk.distributedcounter.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class EventCountTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void shouldSerialize() throws Exception {
        assertEquals("{\"id\":\"Test Id\",\"count\":123}", MAPPER.writeValueAsString(new EventCount("Test Id", 123L)));
    }

    @Test
    public void shouldDeserialize() throws Exception {
        assertEquals(new EventCount("Test Id", 123L), MAPPER.readValue("{\"id\":\"Test Id\",\"count\":123}", EventCount.class));
    }
}