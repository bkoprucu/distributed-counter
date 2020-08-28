package org.berk.distributedcounter;


import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.counter.PeriodicDistributingCounter;

/**
 * Application preferences
 * TODO use external configuration file and remove this
 * */
public class Preferences {

    /**
     * CounterManager implementation to use
     *
     * HazelcastCounter.class : Counts directly on Hazelcast
     * PeriodicDistributingCounter.class: Better performing version, syncs counts periodically on Hazelcast
     * */
    public static final Class<? extends Counter> COUNTER_CLASS = PeriodicDistributingCounter.class;

    public static final int SERVER_PORT = 8080;

    // Delay between sync operations in PeriodicDistributingCounter
    public static final int PERIODIC_COUNTER_SYNC_INTERVAL = 500;

}



















































