package org.berk.distributedcounter.rest;

import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.Counter;
import org.berk.distributedcounter.rest.api.EventCount;
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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class CounterResourceTest extends JerseyTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Counter counter;

    @Mock
    HazelcastInstance hazelcastInstance;

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(CounterResource.class, JacksonFeature.class);
        config.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(counter).to(Counter.class);
            }
        });
        return config;
    }

    @Test
    public void shouldIncrementByOne() {
        String eventId = "abc";
        doReturn(5L).when(counter).increment(eq(eventId), any());
        try (Response response = target("counter/count")
                .path(eventId)
                .request(APPLICATION_JSON_TYPE)
                .put(Entity.entity("", APPLICATION_JSON_TYPE))) {
            assertEquals(200, response.getStatus());
            assertEquals("5", response.readEntity(String.class));
        }
        verify(counter).increment(eventId, null);
    }

    @Test
    public void shouldReturnHttpCreatedForNonExistingCounter() {
        String eventId = "abc";
        String requestId = "testRequestId";
        doReturn(null).when(counter).increment(eventId, requestId);
        try (Response response = target("counter/count")
                .path(eventId)
                .queryParam("requestId", requestId)
                .request(APPLICATION_JSON_TYPE)
                .put(Entity.entity("", APPLICATION_JSON_TYPE))) {
            assertEquals(201, response.getStatus());
            assertEquals("", response.readEntity(String.class));
        }
        verify(counter).increment(eventId, requestId);
    }

    @Test
    public void shouldIncrementByGivenAmount() {
        String eventId = "abc";
        String requestId = "testRequestId";
        try (Response response = target("counter/count")
                .path(eventId)
                .queryParam("amount", "5")
                .queryParam("requestId", requestId)
                .request(APPLICATION_JSON_TYPE)
                .put(Entity.entity("", APPLICATION_JSON_TYPE))) {
            assertEquals(200, response.getStatus());
        }
        verify(counter).increment(eventId, 5, requestId);
    }

    @Test
    public void shouldGetCount() {
        String eventId = "abc";
        doReturn(5L)
                .when(counter).getCount(eq(eventId));
        long count = target("counter/count")
                .path(eventId)
                .request()
                .get(Long.class);
        assertEquals(5L, count);
        verify(counter).getCount(eventId);
    }


    @Test
    public void shouldDeleteCounter() {
        String eventId = "abc";
        String requestId = "testRequestId";
        try(Response response = target("counter/count")
                .path(eventId)
                .queryParam("requestId", requestId)
                .request()
                .delete(Response.class)) {
            assertEquals(200, response.getStatus());
            verify(counter).remove(eventId, requestId);
        }
    }

    @Test
    public void shouldGetSize() {
        doReturn(10L)
                .when(counter).getSize();
        long size = target("counter/size")
                .request()
                .get(Long.class);
        assertEquals(10, size);
        verify(counter).getSize();
    }

    @Test
    public void shouldListCounters() {
        List<EventCount> counts = Arrays.asList(new EventCount("first", 10L),
                                                new EventCount("second", 20L));
        doReturn(counts)
                .when(counter).getCounts();
        List<EventCount> list = target("counter/list")
                .request()
                .get(new GenericType<List<EventCount>>() { });
        assertEquals(counts, list);
    }

}