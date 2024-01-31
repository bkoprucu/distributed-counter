package org.berk.distributedcounter.counter;

public class HazelcastEntryProcessorCounterTest extends CounterTestBase<HazelcastEntryProcessorCounter> {


    @Override
    protected HazelcastEntryProcessorCounter createInstance() {
        return new HazelcastEntryProcessorCounter(hazelcastInstance, deduplicator);
    }

}