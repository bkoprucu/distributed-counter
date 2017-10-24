package org.bashar.rest;

import org.bashar.counter.Counter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("home")
@Singleton
public class Resource {

    private final Counter counter;

    @Inject
    public Resource(Counter counter) {
        this.counter = counter;
    }

    @POST
    @Path("/increment")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public long increment(final String user) {
        return counter.increment(user);
    }

    @GET
    @Path("/getcount")
    @Produces(MediaType.APPLICATION_JSON)
    public Long getCount(@QueryParam("user") final String user) {
        return counter.getCount(user);
    }

    @GET
    @Path("/distribute")
    public void distribute() {
        counter.distribute();
    }


}

