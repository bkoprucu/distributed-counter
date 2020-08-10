package org.berk.distributedcounter.rest;

import org.berk.distributedcounter.api.EventCount;
import org.berk.distributedcounter.api.EventId;
import org.berk.distributedcounter.counter.Counter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("counter")
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
public class CounterResource {

    private final Counter<String> counter;

    @Inject
    public CounterResource(Counter<String> counter) {
        this.counter = counter;
    }

    @PUT
    @Path("/increment")
    public Response increment(@Valid EventId eventId) {
        counter.increment(eventId.getId());
        return Response.ok().build();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public EventCount getCount(@NotEmpty(message = "Parameter 'event_id' is mandatory")
                               @QueryParam("event_id") final String eventId) {
        return new EventCount(eventId, counter.getCount(eventId));
    }


    @GET
    @Path("/counters")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventCount> listAllCounters(@QueryParam("from") Integer from,
                                            @QueryParam("to") Integer to) {
        if (from != null && from < 0) {
            throw new IllegalArgumentException("Parameter 'from' cannot be negative");
        }
        if (to != null) {
            if (to < 0)
                throw new IllegalArgumentException("Parameter 'from' cannot be negative");
            if (from != null && from > to)
                throw new IllegalArgumentException("Parameter 'from' must have smaller value than 'to'");
        }
        return counter.listAllCounters(from, to);
    }

    @GET
    @Path("/listsize")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getSize() {
        return counter.getSize();
    }


}

