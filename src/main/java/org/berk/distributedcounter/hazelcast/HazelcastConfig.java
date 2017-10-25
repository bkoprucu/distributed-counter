package org.berk.distributedcounter.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;

import java.util.List;


public class HazelcastConfig {
    private static final int EXECUTOR_QUEUE_CAPACITY = 100_000;
    private static final String GROUP_NAME = "DistributedCounter_Group";
    private static final String INSTANCE_NAME = "DistributedCounter_Instance";

    private static Config baseConfig() {
        Config config = new Config(INSTANCE_NAME);
        config.getGroupConfig().setName(GROUP_NAME);
        config.setProperty("hazelcast.shutdownhook.policy", "GRACEFUL");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getExecutorConfig("DistributedCounter_Executor")
              .setPoolSize(Runtime.getRuntime().availableProcessors())
              .setQueueCapacity(EXECUTOR_QUEUE_CAPACITY);

        String portStr = System.getProperty("hazelcastPort");
        if(portStr != null) {
            config.getNetworkConfig()
                  .setPort(Integer.parseInt(portStr));
        }

        return config;
    }

    /** Configure Hazelcast for static member discovery */
    public static Config staticTcpDiscovery(List<String> members) {
        Config config = baseConfig();
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig()
                  .setEnabled(false);
        joinConfig.getTcpIpConfig()
                     .setEnabled(true)
                     .setMembers(members);
        return config;
    }


    /** Configure Hazelcast for multicast discovery */
    public static Config multicastDiscovery() {
        Config config = baseConfig();
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig()
                  .setEnabled(true);
        return config;
    }

}
