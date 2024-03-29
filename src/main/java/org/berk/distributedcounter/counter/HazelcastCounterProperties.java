package org.berk.distributedcounter.counter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNullElse;


/**
 * External configuration class for the counter
 */
@ConfigurationProperties(prefix = "hazelcastcounter")
public record HazelcastCounterProperties(String instanceName,
                                         String clusterName,
                                         String kubernetesServiceName,
                                         Integer deduplicationMapTimeoutSecs,
                                         String implementationClassName) {
    static final String DEFAULT_INSTANCE_NAME = "distributedcounter";
    static final String DEFAULT_CLUSTER_NAME = "distributedCounter_cluster";
    static final int    DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS = 600;


    public HazelcastCounterProperties(String instanceName,
                                      String clusterName,
                                      String kubernetesServiceName,
                                      Integer deduplicationMapTimeoutSecs,
                                      String implementationClassName) {
        this.instanceName = requireNonNullElse(instanceName, DEFAULT_INSTANCE_NAME);
        this.clusterName = requireNonNullElse(clusterName, DEFAULT_CLUSTER_NAME);
        this.kubernetesServiceName = kubernetesServiceName;
        this.deduplicationMapTimeoutSecs = requireNonNullElse(deduplicationMapTimeoutSecs, DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS);
        this.implementationClassName = requireNonNullElse(implementationClassName, HazelcastEntryProcessorCounter.class.getSimpleName());
    }

}
