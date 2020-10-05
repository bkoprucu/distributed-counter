package org.berk.distributedcounter.testapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Arguments / options used to run performance test
 */
@AllArgsConstructor
@Builder
@Value
class TestArgs {
    /** Address of the host running the service, e.g. http://localhost:8080 */
    String   serviceHost;

    /** How many items to count */
    int      counters;

    /** How much to increment each item */
    int      incrementBy;

    boolean  keepCountersAfterTest;
    boolean  verifyCountsAfterTest;
    /** Whether or not to send warmup requests before the actual test */
    boolean  warmupEnabled;

    /** Timeout for test */
    Duration timeOut;

    /** Max req / sec to send */
    int speedLimit;
}
