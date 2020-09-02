package org.berk.distributedcounter;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.hazelcast.HazelcastConfig;
import org.berk.distributedcounter.hazelcast.HazelcastCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.config.EnableWebFlux;

@Configuration
@EnableWebFlux
public class SpringConfig {

    private final Logger log = LoggerFactory.getLogger(SpringConfig.class);

    @Bean
    @Lazy(false)
    Counter counter(HazelcastInstance hazelcastInstance) {
        Counter counter = new HazelcastCounter(hazelcastInstance);
        log.info("Configured Counter implementation: {}", counter.getClass().getSimpleName());
        return counter;
    }

    @Bean
    @Lazy(false)
    Config hazelcastConfig() {
        return HazelcastConfig.multicastDiscovery();
    }
}
