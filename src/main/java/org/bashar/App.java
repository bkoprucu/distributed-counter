package org.bashar;

import com.hazelcast.core.HazelcastInstance;
import org.bashar.counter.Counter;
import org.bashar.hazelcast.HazelcastFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        final HazelcastInstance hazelcastInstance = HazelcastFactory.createHazelcastInstance();
        final Counter counter = new Counter(hazelcastInstance);

        final ResourceConfig config = new ResourceConfig();
        config.packages("org.bashar.rest");
        config.register(JacksonFeature.class);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(counter).to(Counter.class);
            }
        });

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        Server server = new Server(2222);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            logger.error("Error in application" ,e);
        } finally {
            server.destroy();
            hazelcastInstance.getLifecycleService().shutdown();
        }
    }
}
