package org.berk.distributedcounter;

import com.hazelcast.config.*;
import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.counter.HazelcastCounter;
import org.berk.distributedcounter.counter.PeriodicDistributingCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@EnableWebFlux
public class SpringConfig implements WebFluxConfigurer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @param usePeriodicDistributingCounter if true use {@link PeriodicDistributingCounter} otherwise use {@link HazelcastCounter}
     * @param syncInterval sync interval for {@link PeriodicDistributingCounter}
     */
    @Bean
    Counter<String> counter(HazelcastInstance hazelcastInstance,
                            @Value("${counter.PeriodicDistributingCounter.enabled:false}") boolean usePeriodicDistributingCounter,
                            @Value("${counter.PeriodicDistributingCounter.syncInterval:1000}") Long syncInterval) {
        if(usePeriodicDistributingCounter) {
            log.info("Using PeriodicDistributingCounter as counter implementation");
            return new PeriodicDistributingCounter<>(hazelcastInstance, syncInterval);
        } else {
            log.info("Using HazelcastCounter as counter implementation");
            return new HazelcastCounter<>(hazelcastInstance);
        }
    }


    @Bean
    Config hazelcastConfig(Environment environment) {
        final int processors = Runtime.getRuntime().availableProcessors();
        Config config = new Config("DistributedCounter_Instance");
        config.setClusterName("DistributedCounter_Group");
        config.setProperty("hazelcast.shutdownhook.policy", "GRACEFUL");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getExecutorConfig("exec").setPoolSize(processors).setQueueCapacity(Integer.MAX_VALUE);
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        if(environment.containsProperty("enableKubernetes")) {
            log.info("Configuring Hazelcast for Kubernetes discovery");
            joinConfig.getMulticastConfig().setEnabled(false);
            joinConfig.getKubernetesConfig().setEnabled(true);
        } else {
            log.info("Configuring Hazelcast for multicast discovery");
            joinConfig.getMulticastConfig().setEnabled(true);
        }

        config.addMapConfig(new MapConfig(HazelcastCounter.MAP_NAME)
                .setEvictionConfig(new EvictionConfig().setEvictionPolicy(EvictionPolicy.NONE))
                .setStatisticsEnabled(false));

        return config;
    }


    /**
     * Adds "Host" header to the response with the hostname / Docker container id
     */
    @Bean
    WebFilter responseHostHeaderFilter() {
        return (exchange, chain) -> {
            try {
                exchange.getResponse().getHeaders().add("Host", InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                log.warn("Cannot resolve hostname of local server / container");
            }
            log.info("Initialized host header filter");
            return chain.filter(exchange);
        };
    }
}
