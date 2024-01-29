package org.berk.distributedcounter.counter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

import static java.util.Objects.requireNonNullElse;


/**
 * External configuration class for the counter
 */
@ConfigurationProperties(prefix = "hazelcastcounter")
public record HazelcastCounterProperties(@Nullable String instanceName,
                                         @Nullable String clusterName,
                                         @Nullable String kubernetesServiceName,
                                         @Nullable Integer deduplicationMapTimeoutSecs) {

    private static final String DEFAULT_INSTANCE_NAME = "distributedcounter";
    private static final String DEFAULT_CLUSTER_NAME = "distributedCounter_cluster";
    private static final int DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS = 600;


    public HazelcastCounterProperties(@Nullable String instanceName,
                                      @Nullable String clusterName,
                                      @Nullable String kubernetesServiceName,
                                      @Nullable Integer deduplicationMapTimeoutSecs) {

        this.instanceName = requireNonNullElse(instanceName, DEFAULT_INSTANCE_NAME);
        this.clusterName = requireNonNullElse(clusterName, DEFAULT_CLUSTER_NAME);
        this.kubernetesServiceName = kubernetesServiceName;
        this.deduplicationMapTimeoutSecs = requireNonNullElse(deduplicationMapTimeoutSecs, DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS);
    }

}
