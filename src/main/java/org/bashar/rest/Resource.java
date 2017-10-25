package org.bashar.rest;

import org.bashar.counter.Counter;
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

@Path("counter")
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
public class Resource {

    private final Counter<String> counter;

    @Inject
    public Resource(Counter<String> counter) {
        this.counter = counter;
    }

    @PUT
    @Path("/increment")
    public void increment(@NotEmpty String user) {
        counter.increment(user);
    }

    @GET
    @Path("/getcount")
    @Produces(MediaType.APPLICATION_JSON)
    public Long getCount(@NotEmpty @QueryParam("user") final String user) {
        return counter.get(user);
    }

}

