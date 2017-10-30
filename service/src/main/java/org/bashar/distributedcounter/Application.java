package org.bashar.distributedcounter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.bashar.distributedcounter.counter.CounterManager;
import org.bashar.distributedcounter.rest.CounterResource;
import org.bashar.distributedcounter.rest.CustomExceptionMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        final HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(
                        HazelcastConfig.getConfig(Preferences.HAZELCAST_PORT, Preferences.HAZELCAST_MEMBERS));

        final ResourceConfig config = new ResourceConfig(CounterResource.class, CustomExceptionMapper.class, JacksonFeature.class);
        //config.packages("org.bashar.rest");
        //config.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(Preferences.COUNTER_MANAGER_CLASS).to(new TypeLiteral<CounterManager<String>>() {
                }.getType());
            }
        });

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));
        Server server = new Server(Preferences.SERVER_PORT);
        server.setStopAtShutdown(true);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            logger.error("SEVERE: Error in application", e);
        } finally {
            server.destroy();
        }
    }
}
