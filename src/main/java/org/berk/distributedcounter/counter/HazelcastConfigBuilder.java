package org.berk.distributedcounter.counter;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

// With version 5.x of Hazelcast, it makes more sense to use the external configuration file hazelcast.yml instead
public final class HazelcastConfigBuilder {

    private final Config config;

    public HazelcastConfigBuilder(String instanceName, String clusterName) {
        this(instanceName, clusterName, null);
    }

    public HazelcastConfigBuilder(String instanceName, String clusterName, @Nullable Integer port) {
        config = new Config(instanceName);
        config.setClusterName(clusterName);
        config.setProperty("hazelcast.shutdownhook.policy", "GRACEFUL");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getExecutorConfig("distributedCounter_executor")
              .setPoolSize(Runtime.getRuntime().availableProcessors());

        if(port != null) { // Default port is 5701
            config.getNetworkConfig()
                  .setPort(port);
        }
    }

    public HazelcastConfigBuilder withMapExpiration(String mapName, int maxIdleSeconds) {
        config.getMapConfig(mapName).setMaxIdleSeconds(maxIdleSeconds);
        return this;
    }

    public HazelcastConfigBuilder withStaticTcpDiscovery(List<String> members) {
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig()
                  .setEnabled(false);
        joinConfig.getTcpIpConfig()
                     .setEnabled(true)
                     .setMembers(members);
        return this;
    }


    public HazelcastConfigBuilder withMulticastDiscovery() {
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig()
                  .setEnabled(true);
        return this;
    }

    public HazelcastConfigBuilder withKubernetesDiscovery(String serviceName, @Nullable String nameSpace) {
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig()
                  .setEnabled(false);
        joinConfig.getKubernetesConfig()
                  .setEnabled(true)
                  .setProperty("service-name", Objects.requireNonNull(serviceName));
        if(nameSpace != null) {
            joinConfig.getKubernetesConfig().setProperty("namespace", nameSpace);
        }
        return this;
    }

    public Config getConfig() {
        return config;
    }
}
