package org.berk.distributedcounter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.berk.distributedcounter.hazelcast.HazelcastConfig;
import org.berk.distributedcounter.hazelcast.HazelcastCounter;
import org.berk.distributedcounter.rest.CounterResource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.berk.distributedcounter.AppConfig.DEFAULT_SERVER_PORT;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);



    public static void main(String[] args) {

        HazelcastInstance hazelcastInstance =
                HazelcastInstanceFactory.getOrCreateHazelcastInstance(HazelcastConfig.multicastDiscovery());

        ResourceConfig config = new ResourceConfig(CounterResource.class, JacksonFeature.class);

        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(HazelcastCounter.class).to(Counter.class);
            }
        });

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));
        Server server = new Server(getServerPort());
        server.setStopAtShutdown(true);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            log.error("Error in application", e);
        } finally {
            server.destroy();
        }
    }


    private static int getServerPort() {
        String portStr = System.getProperty("serverPort");
        if (portStr != null) {
            return Integer.parseInt(portStr);
        }
        return DEFAULT_SERVER_PORT;
    }

}
