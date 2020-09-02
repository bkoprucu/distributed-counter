package org.berk.distributedcounter;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.counter.HazelcastConfigBuilder;
import org.berk.distributedcounter.counter.HazelcastCounter;
import org.berk.distributedcounter.counter.HazelcastCounterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.util.stream.Stream;

@Configuration
@EnableWebFlux
@ConfigurationPropertiesScan(basePackageClasses = HazelcastCounterProperties.class)
public class SpringConfig {

    private final Logger log = LoggerFactory.getLogger(SpringConfig.class);

    @Bean
    @Lazy(false)
    Counter counter(HazelcastInstance hazelcastInstance, HazelcastCounterProperties counterProperties) {
        Counter counter = new HazelcastCounter(hazelcastInstance, counterProperties);
        log.info("Configured Counter implementation: {}", counter.getClass().getSimpleName());
        return counter;
    }

    @Bean
    @Lazy(false)
    Config hazelcastConfig(Environment environment, HazelcastCounterProperties counterProperties) {
        HazelcastConfigBuilder configBuilder = new HazelcastConfigBuilder(counterProperties.clusterName());
        log.info("Configuring Hazelcast for multicast discovery");
        return configBuilder.withMulticastDiscovery()
                            .getConfig();
    }
}
