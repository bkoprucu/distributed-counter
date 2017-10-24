package org.berk.distributedcounter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Application preferences
 * TODO use external configuration file and remove this
 * */
public class Preferences {

    public static final int SERVER_PORT = 8080;

    // Hazelcast will use more ports incrementally (9501, 9502,...)
    public static final int HAZELCAST_PORT = 9500;

    public static final List<String> HAZELCAST_MEMBERS = Collections.unmodifiableList(Arrays.asList(
            // Put all the nodes of cluster here
            // Kubernetes discovery is possible but not configured yet
            "localhost"
    ));
}
