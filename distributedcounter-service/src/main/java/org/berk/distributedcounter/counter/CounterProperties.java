package org.berk.distributedcounter.counter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;


/**
 * External configuration class, for the counter
 * Doesn't follow the usual bean notation ("getXXX() method names, or use Lombok, instead uses Java 14 notation for future compatibility
 */
@ConfigurationProperties(prefix = "counter")
@ConstructorBinding
public class CounterProperties {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final Duration DEFAULT_SYNC_INTERVAL = Duration.ofSeconds(1);

    /**
     * Name of the instance and Hazelcast group
     */
    private final String groupName;

    private final String description;

    /**
     * Use {@link LocalCachingHazelcastCounter} which has local caching capability, otherwise use {@link HazelcastCounter}
     */
    private final boolean useLocalCaching;

    /**
     * Sync interval for {@link LocalCachingHazelcastCounter}, ignored if {@link #useLocalCaching} is "false"
     */
    private final Duration localCacheSyncInterval;


    public CounterProperties(String groupName, String description, boolean useLocalCaching, Duration localCacheSyncInterval) {
        this.groupName = Objects.requireNonNull(groupName);
        this.description = description;
        this.useLocalCaching = useLocalCaching;
        this.localCacheSyncInterval = Optional.ofNullable(localCacheSyncInterval)
                .orElseGet(() -> {
                    log.warn("localCacheSyncInterval not defined in the configuration, using default: {}", DEFAULT_SYNC_INTERVAL);
                    return DEFAULT_SYNC_INTERVAL;
                });
    }

    // Not "getGroupName()", using Java 14 Record notation
    public String groupName() {
        return groupName;
    }

    public String description() {
        return description;
    }

    public boolean useLocalCaching() {
        return useLocalCaching;
    }

    public Duration localCacheSyncInterval() {
        return localCacheSyncInterval;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CounterProperties that = (CounterProperties) o;
        return useLocalCaching == that.useLocalCaching &&
                Objects.equals(log, that.log) &&
                Objects.equals(groupName, that.groupName) &&
                Objects.equals(description, that.description) &&
                Objects.equals(localCacheSyncInterval, that.localCacheSyncInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(log, groupName, description, useLocalCaching, localCacheSyncInterval);
    }

    @Override
    public String toString() {
        return "CounterProperties{" +
                "groupName='" + groupName + '\'' +
                ", description='" + description + '\'' +
                ", useLocalCaching=" + useLocalCaching +
                ", localCacheSyncInterval=" + localCacheSyncInterval +
                '}';
    }
}
