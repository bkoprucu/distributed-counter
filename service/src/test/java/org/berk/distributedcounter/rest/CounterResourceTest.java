package org.berk.distributedcounter.rest;

import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.api.EventCount;
import org.berk.distributedcounter.counter.Counter;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class CounterResourceTest extends JerseyTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Counter<String> counter;
    @Mock
    HazelcastInstance hazelcastInstance;

    private static final GenericType<EventCount> EVENT_COUNT_STRING_TYPE = new GenericType<>(){};
    private static final GenericType<List<EventCount>> LIST_OF_EVENT_COUNT_TYPE = new GenericType<>(){};

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(CounterResource.class, CustomExceptionMapper.class, JacksonFeature.class);
        config.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(counter).to(new TypeLiteral<Counter<String>>() {
                }.getType());
            }
        });
        return config;
    }

    @Test
    public void increment() {
        int status = target("counter/increment/abc").request(APPLICATION_JSON_TYPE)
                .put(Entity.entity("", APPLICATION_JSON_TYPE))
                .getStatus();
        assertEquals(200, status);
    }

    @Test
    public void getCount() {
        doReturn(5L).when(counter).getCount(eq("user1"));
        EventCount eventCount =  target("counter/count/user1")
                .request(APPLICATION_JSON_TYPE).get(EVENT_COUNT_STRING_TYPE);
        assertEquals(new EventCount("user1", 5L), eventCount);
    }

    @Test
    public void getSize() {
        doReturn(3).when(counter).getSize();
        Integer size =  target("counter/listsize")
                .request(APPLICATION_JSON_TYPE)
                .get(Integer.class);
        assertEquals(3, size.intValue());
    }

    @Test
    public void getCountersList() {
        when(counter.listCounters(null , null)).thenReturn(
                List.of(new EventCount("user1", 1L),
                        new EventCount("user2", 2L)));
        List<EventCount> counters = target("counter/list")
                .request(APPLICATION_JSON_TYPE).get(LIST_OF_EVENT_COUNT_TYPE);
        assertEquals(2, counters.size());
        assertEquals(new EventCount("user1", 1L), counters.get(0));
        assertEquals(new EventCount("user2", 2L), counters.get(1));
    }

    @Test
    public void getCountersList_should_reject_negative_item_count() {
        when(counter.listCounters(null , null)).thenReturn(
                List.of(new EventCount("user1", 1L),
                        new EventCount("user2", 2L)));
        Response response = target("counter/list")
                .queryParam("from_index", 1)
                .queryParam("item_count", -4)
                .request(APPLICATION_JSON_TYPE)
                .get();
        assertEquals(400, response.getStatus());
        System.out.println(response.getEntity());
    }


    @Test
    public void errorHandling() {
        when(counter.getCount(anyString())).thenThrow(new IllegalArgumentException());
        Response response =  target("counter/count/user1")
                .request(APPLICATION_JSON_TYPE).get();
        assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());
    }

    //TODO other cases
}