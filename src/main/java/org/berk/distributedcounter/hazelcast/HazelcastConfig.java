package org.berk.distributedcounter.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;

import java.util.List;


public class HazelcastConfig {

    private static final String GROUP_NAME = "DistributedCounter_Group";
    private static final String GROUP_PASSWORD = "DistributedCounter";
    private static final String INSTANCE_NAME = "DistributedCounter_Instance";

    public static Config newConfig(int port, List<String> members) {
        return newConfig(INSTANCE_NAME, port, members);
    }

    public static Config newConfig(String instanceName, int port, List<String> members) {
        Config config = new Config(instanceName);
        config.getGroupConfig().setName(GROUP_NAME).setPassword(GROUP_PASSWORD);
        config.setProperty("hazelcast.shutdownhook.policy", "GRACEFUL");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getExecutorConfig("exec")
              .setPoolSize(Runtime.getRuntime().availableProcessors())
              .setQueueCapacity(100_000);
        // Discovery and members. Static for simplicity
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(port)
                     .setPortAutoIncrement(true);
        networkConfig.getJoin().getMulticastConfig()
                     .setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig()
                     .setEnabled(true)
                     .setMembers(members);
        return config;
    }

}
