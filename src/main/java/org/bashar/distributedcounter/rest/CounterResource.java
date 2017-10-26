package org.bashar.distributedcounter.rest;

import org.bashar.distributedcounter.api.EventId;
import org.bashar.distributedcounter.api.EventCount;
import org.bashar.distributedcounter.counter.CounterManager;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("counter")
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
public class CounterResource {

    private final CounterManager<String> counterManager;

    @Inject
    public CounterResource(CounterManager<String> counterManager) {
        this.counterManager = counterManager;
    }

    @PUT
    @Path("/increment")
    public Response increment(@Valid EventId<String> eventId) {
        counterManager.increment(eventId.getId());
        return Response.ok().build();
    }

    @GET
    @Path("/getcount")
    @Produces(MediaType.APPLICATION_JSON)
    public EventCount<String> getCount(@NotEmpty(message = "Parameter 'event_id' is mandatory")
                                           @QueryParam("event_id") final String eventId) {
        return new EventCount<>(eventId, counterManager.getCount(eventId));
    }


    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventCount<String>> listAllCounters(@QueryParam("from") Integer from,
                                                    @QueryParam("to") Integer to) {
        if(from != null && from < 0) {
            throw new IllegalArgumentException("Parameter 'from' cannot be negative");
        }
        if(to != null) {
            if(to < 0)
                throw new IllegalArgumentException("Parameter 'from' cannot be negative");
            if(from != null && from > to)
                throw new IllegalArgumentException("Parameter 'from' must have smaller value than 'to'");
        }
        return counterManager.listAllCounters(from, to);
    }

    @GET
    @Path("/listsize")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getSize() {
        return counterManager.getSize();
    }

}

