package org.bashar;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;

import java.util.Arrays;
import java.util.List;

public enum HazelcastConfig {
    INSTANCE;


    private  final int port = 9500;
    private final List<String> members = Arrays.asList("localhost");

    private  final String groupName = "DistributedCounters";
    private final String groupPassword = "DistributedCounters";

    private final Config config = hazelCastConfig(port, groupName, groupPassword, members);


    private Config hazelCastConfig(int port, String groupName, String groupPassword, List<String> members) {
        final Config config = new Config("localhost");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.getGroupConfig().setName(groupName).setPassword(groupPassword);

        // Discovery and members. Static for simplicity
        final NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(port).setPortAutoIncrement(true);
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(true).setMembers(members);
        return config;
    }


    public Config getConfig() {
        return config;
    }
}
