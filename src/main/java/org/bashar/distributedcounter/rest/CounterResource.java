package org.bashar.distributedcounter.rest;

import org.bashar.distributedcounter.counter.CounterManager;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    public Response increment(@NotEmpty String counterId) {
        counterManager.increment(counterId);
        return Response.ok().build();
    }

    @GET
    @Path("/getcount")
    @Produces(MediaType.APPLICATION_JSON)
    public Long getCount(@NotEmpty @QueryParam("counterid") final String counterId) {
        return counterManager.getCount(counterId);
    }


    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> listAllCounters(@QueryParam("from") Integer from, @QueryParam("to") Integer to) {
        return counterManager.listAllCounters(from, to);
    }

    @GET
    @Path("/listsize")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getSize() {
        return counterManager.getSize();
    }

}

