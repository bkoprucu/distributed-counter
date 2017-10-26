package org.bashar.distributedcounter.rest;

import com.hazelcast.core.HazelcastInstance;
import org.bashar.distributedcounter.api.EventCount;
import org.bashar.distributedcounter.api.EventId;
import org.bashar.distributedcounter.counter.CounterManager;
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
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class CounterResourceTest extends JerseyTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    CounterManager<String> counterManager;
    @Mock
    HazelcastInstance hazelcastInstance;

    private static final GenericType<EventCount<String>> EVENT_COUNT_STRING_TYPE = new GenericType<EventCount<String>>(){};
    private static final GenericType<List<EventCount<String>>> LIST_OF_EVENT_COUNT_STRING_TYPE = new GenericType<List<EventCount<String>>>(){};

    @Override
    protected Application configure() {

        final ResourceConfig config = new ResourceConfig(CounterResource.class, CustomExceptionMapper.class, JacksonFeature.class);
        config.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(counterManager).to(new TypeLiteral<CounterManager<String>>(){}.getType());
            }
        });
        return config;
    }

    @Test
    public void increment() throws Exception {
        int status =  target("counter/increment").request(APPLICATION_JSON_TYPE)
                .put(Entity.entity(new EventId<String>("abc"), APPLICATION_JSON_TYPE))
                .getStatus();
        assertEquals(200, status);
    }

    @Test
    public void getCount() throws Exception {
        doReturn(5L).when(counterManager).getCount(eq("user1"));
        EventCount<String> eventCount =  target("counter/getcount").queryParam("event_id", "user1")
                .request(APPLICATION_JSON_TYPE).get(EVENT_COUNT_STRING_TYPE);
        assertEquals(  new EventCount<String>("user1", 5L), eventCount);
    }

    @Test
    public void getSize() throws Exception {
        doReturn(3).when(counterManager).getSize();
        Integer size =  target("counter/listsize")
                .request(APPLICATION_JSON_TYPE).get(Integer.class);
        assertEquals(3, size.intValue());
    }

    @Test
    public void listAllCounters() throws Exception {
        when(counterManager.listAllCounters(null , null)).thenReturn(
                Arrays.asList(new EventCount<String>("user1", 1L),
                        new EventCount<String>("user2", 2L)));
        List<EventCount<String>> counters = target("counter/list")
                .request(APPLICATION_JSON_TYPE).get(new GenericType<List<EventCount<String>>>() {});
        assertEquals(2, counters.size());
        assertEquals(new EventCount<>("user1", 1L), counters.get(0));
        assertEquals(new EventCount<>("user2", 2L), counters.get(1));
    }

    @Test
    public void errorHandling() throws Exception {
        when(counterManager.getCount(anyString())).thenThrow(new IllegalArgumentException());
        Response response =  target("counter/getcount").queryParam("event_id", "user1")
                .request(APPLICATION_JSON_TYPE).get();
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());
    }

    //TODO other cases
}