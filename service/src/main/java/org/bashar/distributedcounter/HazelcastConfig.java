package org.bashar.distributedcounter;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;

import java.util.List;


/**
 * Hazelcast preferences.
 * TODO refactor, use external config
 */
public class HazelcastConfig {


    private static final String GROUP_NAME = "DistributedCounter_Group";
    private static final String GROUP_PASSWORD = "DistributedCounter";
    static final String HAZELCAST_INSTANCE_NAME = "DistributedCounter_Instance";


    public static Config getConfig(int port, List<String> members) {
        return getConfig(HAZELCAST_INSTANCE_NAME, port, members);
    }

    public static Config getConfig(String instanceName, int port, List<String> members) {
        final Config config = new Config(instanceName);
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getGroupConfig().setName(GROUP_NAME).setPassword(GROUP_PASSWORD);

        // Discovery and members. Static for simplicity
        final NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(port).setPortAutoIncrement(true);
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(true).setMembers(members);
        return config;
    }

}

