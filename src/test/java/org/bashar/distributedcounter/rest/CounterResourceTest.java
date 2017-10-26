package org.bashar.distributedcounter.rest;

import com.hazelcast.core.HazelcastInstance;
import org.bashar.distributedcounter.counter.CounterManager;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.Assert.*;
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
        int status =  target("counter/increment").request()
                .put(Entity.entity("user1", MediaType.APPLICATION_JSON_TYPE))
                .getStatus();
        assertEquals(200, status);
    }

    @Test
    public void getCount() throws Exception {
        doReturn(5L).when(counterManager).getCount(eq("user1"));
        Long count =  target("counter/getcount").queryParam("counterid", "user1")
                .request(MediaType.APPLICATION_JSON_TYPE).get(Long.class);
        assertEquals(5L, count.longValue());
    }

    @Test
    public void getSize() throws Exception {
        doReturn(3).when(counterManager).getSize();
        Integer size =  target("counter/listsize")
                .request(MediaType.APPLICATION_JSON_TYPE).get(Integer.class);
        assertEquals(3, size.intValue());
    }

    @Test
    public void listAllCounters() throws Exception {
        when(counterManager.listAllCounters(null , null)).thenReturn(
                new HashMap<String, Long>() {{ put("user1", 1L);put("user2", 2L); }});
        Map<String, Long> counters = target("counter/list")
                .request(MediaType.APPLICATION_JSON_TYPE).get(new GenericType<Map<String,Long>>() {});
        assertEquals(2, counters.size());
        assertEquals(1L, counters.get("user1").longValue());
        assertEquals(2L, counters.get("user2").longValue());
    }

    @Test
    public void errorHandling() throws Exception {
        when(counterManager.getCount(anyString())).thenThrow(new IllegalArgumentException());
        Response response =  target("counter/getcount").queryParam("counterid", "user1")
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    //TODO other cases
}