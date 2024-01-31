package org.berk.distributedcounter.counter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;

public abstract class HazelcastTest {
    protected static final HazelcastInstance hazelcastInstance =
            HazelcastInstanceFactory.getOrCreateHazelcastInstance(
                    new HazelcastConfigBuilder("testInstance", "testCluster")
                            .withMulticastDiscovery()
                            .getConfig());
}
