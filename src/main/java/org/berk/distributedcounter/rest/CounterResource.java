package org.berk.distributedcounter.rest;

import org.berk.distributedcounter.Counter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


// TODO improve API
@Path("counter")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class CounterResource {

    private final Counter counter;

    @Inject
    public CounterResource(Counter counter) {
        this.counter = counter;
    }

    @PUT
    @Path("{eventId}")
    public Long increment(@PathParam("eventId") String eventId, @QueryParam("amount") Integer amount) {
        return amount == null ? counter.increment(eventId)
                              : counter.increment(eventId, amount);
    }


    @GET
    @Path("{eventId}")
    public Long getCount(@PathParam("eventId") String eventId) {
        return counter.getCount(eventId);
    }
}
