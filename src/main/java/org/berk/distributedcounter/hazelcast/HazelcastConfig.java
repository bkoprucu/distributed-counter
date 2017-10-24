package org.berk.distributedcounter.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;

import java.util.List;


public class HazelcastConfig {

    private static final String GROUP_NAME = "DistributedCounter_Group";
    private static final String GROUP_PASSWORD = "DistributedCounter";
    private static final String INSTANCE_NAME = "DistributedCounter_Instance";

    public static Config getConfig(int port, List<String> members) {
        return getConfig(INSTANCE_NAME, port, members);
    }

    public static Config getConfig(String instanceName, int port, List<String> members) {
        final int processors = Runtime.getRuntime().availableProcessors();
        Config config = new Config(instanceName);
        config.getGroupConfig().setName(GROUP_NAME).setPassword(GROUP_PASSWORD);
        config.setProperty("hazelcast.shutdownhook.policy", "GRACEFUL");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getExecutorConfig("exec").setPoolSize(processors * 2).setQueueCapacity(Integer.MAX_VALUE);
        // Discovery and members. Static for simplicity
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(port).setPortAutoIncrement(true);
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(true).setMembers(members);
        return config;
    }

}
