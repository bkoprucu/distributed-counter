package org.bashar.distributedcounter.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;

@Provider
public class CustomExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    // TODO improve exception handling
    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(METHOD_NOT_ALLOWED).entity(exception.getMessage()).type(APPLICATION_JSON_TYPE).build();
    }
}
