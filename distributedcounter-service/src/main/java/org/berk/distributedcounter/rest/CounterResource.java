package org.berk.distributedcounter.rest;

import org.berk.distributedcounter.api.Count;
import org.berk.distributedcounter.counter.Counter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.UnknownHostException;
import java.util.List;

@Path("counter")
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CounterResource {

    private final Counter<String> counter;

    @Inject
    public CounterResource(Counter<String> counter) {
        this.counter = counter;
    }

    @PUT
    @Path("/count/{id}")
    public Response increment(@NotEmpty(message = "Parameter 'id' is mandatory") @PathParam("id") String eventId,
                              @QueryParam("amount") @Positive(message = "amount must be positive") Long amount) {
        if(amount == null) {
            counter.increment(eventId);
        } else {
            counter.increment(eventId, amount);
        }
        return Response.ok().build();
    }

    @GET
    @Path("/count/{id}")
    public Count getCount(@NotEmpty(message = "Parameter 'id' is mandatory")
                               @PathParam("id") final String eventId) {
        return new Count(eventId, counter.getCount(eventId));
    }

    @DELETE
    @Path("/count/{id}")
    public void removeCount(@NotEmpty(message = "Parameter 'id' is mandatory")
                            @PathParam("id") final String eventId) {
        counter.removeCounter(eventId);
    }

    @GET
    @Path("/list")
    public List<Count> listCounters(@QueryParam("from_index") Integer fromIndex,
                                    @QueryParam("item_count") @Positive Integer itemCount) throws UnknownHostException {
        return counter.listCounters(fromIndex, itemCount);
    }

    @GET
    @Path("/listsize")
    public Integer getSize() {
        return counter.getSize();
    }

}

