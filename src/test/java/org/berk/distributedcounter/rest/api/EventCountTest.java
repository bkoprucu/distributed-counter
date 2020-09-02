package org.berk.distributedcounter.rest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class EventCountTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void shouldSerialize() throws Exception {
        assertEquals("{\"id\":\"Test Id\",\"count\":123}",
                     MAPPER.writeValueAsString(new EventCount("Test Id", 123L)));
    }

    @Test
    public void shouldDeserialize() throws Exception {
        assertEquals(new EventCount("Test Id", 123L),
                     MAPPER.readValue("{\"id\":\"Test Id\",\"count\":123}", EventCount.class));
    }

    @Test
    public void shouldCompare() {
        EventCount count1 = new EventCount("a1", 10L);
        EventCount count2 = new EventCount("a2", 10L);
        EventCount countBig = new EventCount("big", 11L);

        assertEquals(0, count1.compareTo(count2));
        assertEquals(0, count2.compareTo(count1));
        assertEquals(1, countBig.compareTo(count1));
    }
}