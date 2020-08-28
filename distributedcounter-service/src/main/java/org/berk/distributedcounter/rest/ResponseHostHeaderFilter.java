package org.berk.distributedcounter.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Sets the header "Host" to the name of the Kubernetes pod, for demo and test purpposes
 */
@Provider
public class ResponseHostHeaderFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add("Host", InetAddress.getLocalHost().getHostName());
    }
}

