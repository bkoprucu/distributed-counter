package org.berk.distributedcounter.rest;

import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.DistributedCounterApp;
import org.berk.distributedcounter.api.Count;
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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CounterResourceTest extends JerseyTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Counter<String> counter;

    @Mock
    HazelcastInstance hazelcastInstance;

    private static final GenericType<Count> EVENT_COUNT_STRING_TYPE = new GenericType<>(){};
    private static final GenericType<List<Count>> LIST_OF_EVENT_COUNT_TYPE = new GenericType<>(){};

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = DistributedCounterApp.resourceConfig();
        resourceConfig.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(counter).to(new TypeLiteral<Counter<String>>() {
                }.getType());
            }
        });
        return resourceConfig;
    }

    @Test
    public void increment() {
        int status = target("counter/count/abc").request(APPLICATION_JSON_TYPE)
                .put(Entity.entity("", APPLICATION_JSON_TYPE))
                .getStatus();
        verify(counter).increment("abc");
        assertEquals(200, status);
    }

    @Test
    public void get_count() {
        doReturn(5L).when(counter).getCount(eq("user1"));
        Count count =  target("counter/count/user1")
                .request(APPLICATION_JSON_TYPE).get(EVENT_COUNT_STRING_TYPE);
        assertEquals(new Count("user1", 5L), count);
    }


    @Test
    public void remove_counter() {
        Response response = target("counter/count/event1")
                .request(APPLICATION_JSON_TYPE)
                .delete();
        verify(counter).removeCounter("event1");
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.Family.familyOf(response.getStatus()));
    }

    @Test
    public void get_list_size() {
        doReturn(3).when(counter).getSize();
        Integer size =  target("counter/listsize")
                .request(APPLICATION_JSON_TYPE)
                .get(Integer.class);
        assertEquals(3, size.intValue());
    }


    @Test
    public void provide_host_header() {
        doReturn(3).when(counter).getSize();
        Response response =  target("counter/listsize")
                .request(APPLICATION_JSON_TYPE)
                .get();
        assertNotNull(response.getHeaders().get("Host"));
    }

    @Test
    public void get_counter_list() {
        when(counter.listCounters(null , null)).thenReturn(
                List.of(new Count("user1", 1L),
                        new Count("user2", 2L)));
        List<Count> counters = target("counter/list")
                .request(APPLICATION_JSON_TYPE).get(LIST_OF_EVENT_COUNT_TYPE);
        assertEquals(2, counters.size());
        assertEquals(new Count("user1", 1L), counters.get(0));
        assertEquals(new Count("user2", 2L), counters.get(1));
    }

    @Test
    public void getCountersList_should_reject_negative_item_count() {
        when(counter.listCounters(null , null)).thenReturn(
                List.of(new Count("user1", 1L),
                        new Count("user2", 2L)));
        Response response = target("counter/list")
                .queryParam("from_index", 1)
                .queryParam("item_count", -4)
                .request(APPLICATION_JSON_TYPE)
                .get();
        assertEquals(400, response.getStatus());
    }

    @Test
    public void handle_errors() {
        when(counter.getCount(anyString())).thenThrow(new IllegalArgumentException());
        Response response =  target("counter/count/user1")
                .request(APPLICATION_JSON_TYPE).get();
        assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON_TYPE, response.getMediaType());
    }

}