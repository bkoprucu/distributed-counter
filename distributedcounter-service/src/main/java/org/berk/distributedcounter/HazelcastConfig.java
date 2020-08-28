package org.berk.distributedcounter;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;

import java.util.List;


/**
 * Hazelcast preferences.
 */
public class HazelcastConfig {

    private static final String GROUP_NAME = "DistributedCounter_Group";
    private static final String INSTANCE_NAME = "DistributedCounter_Instance";


    public static Config getConfig(boolean kubernetesEnabled) {
        final int processors = Runtime.getRuntime().availableProcessors();
        Config config = new Config(INSTANCE_NAME);
        config.setClusterName(GROUP_NAME);
        config.setProperty("hazelcast.shutdownhook.policy", "GRACEFUL");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getExecutorConfig("exec").setPoolSize(processors).setQueueCapacity(Integer.MAX_VALUE);
        // Discovery and members. Static for simplicity
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        if(kubernetesEnabled) {
            joinConfig.getMulticastConfig().setEnabled(false);
            joinConfig.getKubernetesConfig().setEnabled(true);
        } else {
            joinConfig.getMulticastConfig().setEnabled(true);
        }
        return config;
    }

}
