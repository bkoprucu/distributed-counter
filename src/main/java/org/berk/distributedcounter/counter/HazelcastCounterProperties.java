package org.berk.distributedcounter.counter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.lang.Nullable;


/**
 * External configuration class, for the counter
 * Uses Java records notation for future compatibility, instead of bean notation
 */
@ConfigurationProperties(prefix = "hazelcastcounter")
@ConstructorBinding
public class HazelcastCounterProperties {

    private static final String DEFAULT_INSTANCE_NAME = "distributedcounter";
    private static final String DEFAULT_CLUSTER_NAME = "distributedCounter_cluster";
    private static final int DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS = 600;

    private final String instanceName;

    private final String clusterName;

    private final Integer deduplicationMapTimeoutSecs;


    public HazelcastCounterProperties(@Nullable String instanceName,
                                      @Nullable String clusterName,
                                      @Nullable Integer deduplicationMapTimeoutSecs) {
        this.instanceName = instanceName == null ? DEFAULT_INSTANCE_NAME
                                                 : instanceName;
        this.clusterName = clusterName == null ? DEFAULT_CLUSTER_NAME
                                               : clusterName;
        this.deduplicationMapTimeoutSecs =
                deduplicationMapTimeoutSecs == null ? DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS
                                                    : deduplicationMapTimeoutSecs;
    }

    public String instanceName() {
        return instanceName;
    }

    public String clusterName() {
        return clusterName;
    }


    public Integer getDeduplicationMapTimeOutSecs() {
        return deduplicationMapTimeoutSecs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HazelcastCounterProperties that = (HazelcastCounterProperties) o;

        if (!instanceName.equals(that.instanceName)) return false;
        if (!clusterName.equals(that.clusterName)) return false;
        return deduplicationMapTimeoutSecs.equals(that.deduplicationMapTimeoutSecs);
    }

    @Override
    public int hashCode() {
        int result = instanceName.hashCode();
        result = 31 * result + clusterName.hashCode();
        result = 31 * result + deduplicationMapTimeoutSecs.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "HazelcastCounterProperties{" +
                "instanceName='" + instanceName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", deduplicationMapTimeOutSecs=" + deduplicationMapTimeoutSecs +
                '}';
    }
}
