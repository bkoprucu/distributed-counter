package org.bashar.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;

import java.util.Arrays;
import java.util.List;

public class HazelcastFactory {

    private static final List<String> MEMBERS = Arrays.asList("localhost"); //TODO configure members


    private static Config hazelCastConfig(int port, String groupName, String groupPassword, List<String> members) {
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

    public static HazelcastInstance createHazelcastInstance() {
        Config config = hazelCastConfig(9500, "distributedcounter.1", "counter", MEMBERS);
        return HazelcastInstanceFactory.getOrCreateHazelcastInstance(config);
    }
}
