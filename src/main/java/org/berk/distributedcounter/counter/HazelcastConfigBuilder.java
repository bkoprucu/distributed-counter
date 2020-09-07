package org.berk.distributedcounter.counter;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import org.springframework.lang.Nullable;

import java.util.List;


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

        if(port != null) {
            config.getNetworkConfig()
                  .setPort(port);
        }
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

    public HazelcastConfigBuilder withKubernetesDiscovery() {
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig()
                  .setEnabled(false);
        joinConfig.getKubernetesConfig()
                  .setEnabled(true);
        return this;
    }

    public Config getConfig() {
        return config;
    }
}
