package org.berk.distributedcounter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import org.berk.distributedcounter.counter.Counter;
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

import java.util.Optional;

public class DistributedCounterApp {

    private static final Logger logger = LoggerFactory.getLogger(DistributedCounterApp.class);

    public static void main(String[] args) {

        HazelcastInstance hazelcastInstance = HazelcastInstanceFactory.getOrCreateHazelcastInstance(
                HazelcastConfig.getConfig(Optional.ofNullable(System.getProperty("enableKubernetes")).isPresent()));

        ResourceConfig resourceConfig = resourceConfig().register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(hazelcastInstance).to(HazelcastInstance.class);
                bind(Preferences.COUNTER_CLASS).to(new TypeLiteral<Counter<String>>() {}.getType());
            }
        });

        ServletHolder servlet = new ServletHolder(new ServletContainer(resourceConfig));
        Server server = new Server(Preferences.SERVER_PORT);
        server.setStopAtShutdown(true);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");

        logger.info("Selected CounterManager={}", Preferences.COUNTER_CLASS.getSimpleName());

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            logger.error("SEVERE: Error in application", e);
        } finally {
            server.destroy();
        }
    }


    public static ResourceConfig resourceConfig() {
        ResourceConfig resourceConfig = new ResourceConfig(JacksonFeature.class);
        resourceConfig.packages("org.berk.distributedcounter.rest");
        return resourceConfig;
    }

}
