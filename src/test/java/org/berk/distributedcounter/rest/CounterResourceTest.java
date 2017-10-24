package org.berk.distributedcounter.rest;

import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.Counter;
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
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.*;
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
    public void incrementByOne() {
        String eventId = "abc";
        try (Response response = target("counter")
                .path(eventId)
                .request(APPLICATION_JSON_TYPE)
                .put(Entity.entity("", APPLICATION_JSON_TYPE))) {
            assertEquals(200, response.getStatus());
        }
        verify(counter).increment(eventId);
    }

    @Test
    public void incrementByGivenAmount() {
        String eventId = "abc";

        try (Response response = target("counter")
                .path(eventId)
                .queryParam("amount", "5")
                .request(APPLICATION_JSON_TYPE)
                .put(Entity.entity("", APPLICATION_JSON_TYPE))) {
            assertEquals(200, response.getStatus());
        }
        verify(counter).increment(eventId, 5);
    }

    @Test
    public void getCount() {
        String eventId = "abc";
        doReturn(5L)
                .when(counter).getCount(eq(eventId));
        long count = target("counter")
                .path(eventId)
                .request()
                .get(Long.class);
        assertEquals(5L, count);
        verify(counter).getCount(eventId);
    }

}