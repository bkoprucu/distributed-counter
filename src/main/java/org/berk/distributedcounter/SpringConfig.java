package org.berk.distributedcounter;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.counter.Deduplicator;
import org.berk.distributedcounter.counter.HazelcastConfigBuilder;
import org.berk.distributedcounter.counter.HazelcastEntryProcessorCounter;
import org.berk.distributedcounter.counter.HazelcastCounterProperties;
import org.berk.distributedcounter.counter.HazelcastPNCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.util.stream.Stream;

import static org.berk.distributedcounter.counter.Deduplicator.DEDUPLICATION_MAP_NAME;

@Configuration
@EnableWebFlux
@ConfigurationPropertiesScan(basePackageClasses = HazelcastCounterProperties.class)
public class SpringConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringConfig.class);

    @Bean
    @Lazy(false)
    Counter counter(HazelcastInstance hazelcastInstance, Deduplicator deduplicator, HazelcastCounterProperties counterProperties) {
        Counter counter = counterProperties.implementationClassName().equalsIgnoreCase(HazelcastPNCounter.class.getSimpleName())
                ? new HazelcastPNCounter(hazelcastInstance, deduplicator)
                : new HazelcastEntryProcessorCounter(hazelcastInstance, deduplicator);
        log.info("Configured Counter implementation: {}", counter.getClass().getSimpleName());
        return counter;
    }

    @Bean
    @Lazy(false)
    Config hazelcastConfig(Environment environment, HazelcastCounterProperties counterProperties) {
        HazelcastConfigBuilder configBuilder =
                new HazelcastConfigBuilder(counterProperties.instanceName(), counterProperties.clusterName())
                        .withMapExpiration(DEDUPLICATION_MAP_NAME,
                                           counterProperties.deduplicationMapTimeoutSecs());
        if (Stream.of(environment.getActiveProfiles()).anyMatch(profile -> profile.equalsIgnoreCase("kubernetes"))) {
            log.info("Configuring Hazelcast for Kubernetes discovery");
            return configBuilder.withKubernetesDiscovery(counterProperties.kubernetesServiceName(), null)
                                .getConfig();
        }
        log.info("Configuring Hazelcast for multicast discovery");
        return configBuilder.withMulticastDiscovery()
                            .getConfig();
    }

}
