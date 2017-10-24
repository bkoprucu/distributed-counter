package org.berk.distributedcounter.rest;

import org.berk.distributedcounter.Counter;
import org.berk.distributedcounter.rest.api.EventCount;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;


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
    @Path("/count/{eventId}")
    public Response increment(@PathParam("eventId") String eventId,
                              @QueryParam("amount") Integer amount) {
        Long previous = amount == null ? counter.increment(eventId)
                                       : counter.increment(eventId, amount);
        return Response.status(previous == null ? CREATED
                                                : OK)
                       .entity(previous).build();
    }


    @GET
    @Path("/count/{eventId}")
    public Long getCount(@PathParam("eventId") String eventId) {
        return counter.getCount(eventId);
    }

    @DELETE
    @Path("/count/{eventId}")
    public Long deleteCount(@PathParam("eventId") String eventId) {
        return counter.remove(eventId);
    }

    @GET
    @Path("/size")
    public Long getSize() {
        return counter.getSize();
    }

    @GET
    @Path("/list")
    public List<EventCount> getCounts() {
        return counter.getCounts();
    }

}
