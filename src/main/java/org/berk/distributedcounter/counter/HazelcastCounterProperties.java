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

    private static final int DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS = 600;
    private static final String DEFAULT_CLUSTER_NAME = "DistributedCounter_Cluster";


    /**
     * Name of the instance and Hazelcast group
     */
    private final String clusterName;

    private final Integer deduplicationMapTimeOutSecs;


    public HazelcastCounterProperties(@Nullable String clusterName, @Nullable Integer deduplicationMapTimeOutSecs) {
        this.clusterName = clusterName == null ? DEFAULT_CLUSTER_NAME
                                               : clusterName;
        this.deduplicationMapTimeOutSecs =
                deduplicationMapTimeOutSecs == null ? DEFAULT_DEDUPLICATION_MAP_TIMEOUT_SECS
                                                    : deduplicationMapTimeOutSecs;
    }

    public String clusterName() {
        return clusterName;
    }


    public Integer getDeduplicationMapTimeOutSecs() {
        return deduplicationMapTimeOutSecs;
    }

    @Override
    public String toString() {
        return "HazelcastCounterProperties{" +
                "clusterName='" + clusterName + '\'' +
                ", deduplicationMapTimeOutSecs=" + deduplicationMapTimeOutSecs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HazelcastCounterProperties that = (HazelcastCounterProperties) o;

        if (!clusterName.equals(that.clusterName)) return false;
        return deduplicationMapTimeOutSecs.equals(that.deduplicationMapTimeOutSecs);
    }

    @Override
    public int hashCode() {
        int result = clusterName.hashCode();
        result = 31 * result + deduplicationMapTimeOutSecs.hashCode();
        return result;
    }
}
