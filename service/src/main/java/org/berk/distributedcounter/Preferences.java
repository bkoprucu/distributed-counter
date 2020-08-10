package org.berk.distributedcounter;


import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.counter.PeriodicDistributingCounter;

import java.util.Collections;
import java.util.List;

/**
 * Application preferences
 * TODO use external configuration file and remove this
 * */
public class Preferences {

    /**
     * CounterManager implementation to use
     *
     * HazelcastCounterManager.class : Counts directly on Hazelcast
     * PeriodicDistributingCounterManager.class: Better performing version, syncs counts periodically on Hazelcast
     * */
    public static final Class<? extends Counter> COUNTER_MANAGER_CLASS = PeriodicDistributingCounter.class;


    public static final int SERVER_PORT = 8080;

    // Hazelcast will use more ports incrementally (9501, 9502,...)
    public static final int HAZELCAST_PORT = 9500;


    public static final List<String> HAZELCAST_MEMBERS = Collections.singletonList(
            // Put all the nodes of cluster here
            // AWS auto discovery is possible but not configured yet
            "localhost"
    );

    // Delay between sync operations in PeriodicDistributingCounter
    public static final int PERIODIC_COUNTER_SYNC_DELAY = 500;

}



















































