package org.berk.distributedcounter.testapp;

import java.time.Duration;

class PerformanceStats {
    final int expectedRequestCount;
    final int successCount;
    final int failCount;
    final int avgReqPerSec;
    final Duration processingDuration;

    public PerformanceStats(int expectedRequestCount, int successCount, int failCount, Duration processingDuration) {
        this.expectedRequestCount = expectedRequestCount;
        this.successCount = successCount;
        this.failCount = failCount;
        this.processingDuration = processingDuration;
        this.avgReqPerSec = (int) (successCount * 1000 / processingDuration.toMillis());
    }

    @Override
    public String toString() {
        return "\n\n\t  Expected requests: " + expectedRequestCount +
               "\n\tSuccessful requests: " + successCount +
               "\n\t    Failed requests: " + failCount +
               "\n\t           Duration: " + processingDuration.toMillis() + " ms" +
               "\n\t              Speed: " + avgReqPerSec + " req/sec";
    }
}
