package org.bashar.distributedcounter;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;

import java.util.Arrays;

//TODO refactor
public class HazelcastConfigFactory {

    private static final int DEFAULT_PORT = 9500;

    private static final String DEFAULT_GROUP_NAME = "DistributedCounter_Group";
    private static final String DEFAULT_GROUP_PASSWORD = "DistributedCounter";
    static final String DEFAULT_INSTANCE_NAME = "DistributedCounter_Instance";



    public static Config hazelCastConfig(String instanceName, String... members) {
        return hazelCastConfig(DEFAULT_PORT, instanceName, DEFAULT_GROUP_NAME, DEFAULT_GROUP_PASSWORD, members);
    }

    /**
     * Create configuration
     *
     * @param port
     * @param groupName
     * @param groupPassword
     * @param members       Hotnames on which application has been deployed
     * @return
     */
    public static Config hazelCastConfig(int port, String instanceName, String groupName, String groupPassword, String... members) {
        final Config config = new Config(instanceName);
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getGroupConfig().setName(groupName).setPassword(groupPassword);

        // Discovery and members. Static for simplicity
        final NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(port).setPortAutoIncrement(true);
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(true).setMembers(Arrays.asList(members));
        return config;
    }
}

