package org.berk.distributedcounter;

import com.hazelcast.config.*;
import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.counter.CounterProperties;
import org.berk.distributedcounter.counter.HazelcastCounter;
import org.berk.distributedcounter.counter.LocalCachingHazelcastCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

@Configuration
@EnableWebFlux
@ConfigurationPropertiesScan(basePackageClasses = CounterProperties.class)
public class SpringConfig implements WebFluxConfigurer {

    private final Logger log = LoggerFactory.getLogger(SpringConfig.class);

    @Bean
    @Lazy(false) // Create the beans defined in this class to serve the requests right away
    Counter<String> counter(HazelcastInstance hazelcastInstance, CounterProperties counterProperties) {
        Counter<String> counter = counterProperties.useLocalCaching()
                ? new LocalCachingHazelcastCounter<>(hazelcastInstance, counterProperties.localCacheSyncInterval())
                : new HazelcastCounter<>(hazelcastInstance);
        log.info("Configured Counter implementation: {}", counter.getClass().getSimpleName());
        return counter;
    }


    @Bean
    @Lazy(false)
    Config hazelcastConfig(Environment environment, CounterProperties counterProperties) {
        Config config = new Config("distributedcounter");
        config.setClusterName(counterProperties.groupName())
                .setProperty("hazelcast.shutdownhook.policy", "GRACEFUL")
                .setProperty("hazelcast.logging.type", "slf4j")
                .getExecutorConfig("exec").setPoolSize(Runtime.getRuntime().availableProcessors());
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        if (Stream.of(environment.getActiveProfiles()).anyMatch(profile -> profile.equalsIgnoreCase("kubernetes"))) {
            log.info("Configuring Hazelcast for Kubernetes discovery");
            joinConfig.getMulticastConfig().setEnabled(false);
            KubernetesConfig kubernetesConfig = joinConfig.getKubernetesConfig();
            kubernetesConfig.setEnabled(true);
        } else {
            log.info("Configuring Hazelcast for multicast discovery");
            joinConfig.getMulticastConfig().setEnabled(true);
        }
        config.addMapConfig(new MapConfig(HazelcastCounter.MAP_NAME)
                .setEvictionConfig(new EvictionConfig().setEvictionPolicy(EvictionPolicy.NONE).setMaxSizePolicy(MaxSizePolicy.FREE_HEAP_PERCENTAGE).setSize(10)));
        return config;
    }


    /**
     * Adds "Host" header to the response with the hostname / Docker container id
     */
    @Bean
    @Lazy(false)
    WebFilter responseHostHeaderFilter() {
        log.info("Initializing host header filter");
        return (exchange, chain) -> {
            try {
                exchange.getResponse().getHeaders().add("Host", InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                log.warn("Cannot resolve hostname of local server / container");
            }
            return chain.filter(exchange);
        };
    }
}
