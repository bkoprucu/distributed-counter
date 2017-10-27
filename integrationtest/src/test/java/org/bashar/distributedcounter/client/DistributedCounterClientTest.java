package org.bashar.distributedcounter.client;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

import org.bashar.distributedcounter.api.EventCount;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;



public class DistributedCounterClientTest {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    private DistributedCounterApacheClient client = DistributedCounterApacheClient.newClient(
            SERVER_HOST, SERVER_PORT, 250, 1000, 5);

    @Test
    public void shouldIncrementAndRead() throws Exception {
        final int count = 10_000;
        String eventId = generateEventIdPrefix();
        IntStream.range(0, count).forEach(i -> client.increment(eventId));
        await().atMost(5, SECONDS).until(() -> count == client.getCount(eventId));
        assertEquals(count, client.getCount(eventId));
    }

    @Test
    public void shouldGetListAndListCount() throws Exception {
        int beginWith = client.getListSize();
        final int count = 100;
        String eventIdPrefix = generateEventIdPrefix();
        List<String> eventIdList = new ArrayList<>(count);
        IntStream.range(0, count).forEach(i -> eventIdList.add(eventIdPrefix + UUID.randomUUID().toString()));

        eventIdList.forEach(eventId -> client.increment(eventId));

        await().atMost(10, SECONDS).until(() -> count + beginWith == client.getListSize());
        List<EventCount> countList = client.getCounters();
        assertEquals(count + beginWith, countList.size());

        if(beginWith == 0) {
            countList.forEach(eventCount -> {
                Assert.assertTrue(eventIdList.contains(eventCount.getId()));
                assertEquals(1L, eventCount.getCount().longValue());
            });
        }
    }

    @Test
    public void shouldHandleLoad() throws Exception {
        final int threads = 4;
        final int events = 20000;

        String prefix = generateEventIdPrefix();

        long start = System.currentTimeMillis();
        IntStream.range(0, threads).forEach(threadId -> IntStream.range(0, events).forEach(eventNumber ->{
            client.increment(prefix + threadId);
        }));
        long end = System.currentTimeMillis() - start;

        System.out.println("Performance: " + threads * events * 1000 / end + " requests / second");
//
//        await().atMost(30, SECONDS).until(() -> client.getListSize() == threads && client.getCount(prefix + 23) == events);
//        IntStream.range(0, threads).forEach(threadId -> IntStream.range(0, events).forEach(eventNumber ->{
//            assertEquals(events, client.getCount(prefix + threadId));
//        }));
    }

    private static String generateEventIdPrefix() {
        return "_test_" + DistributedCounterClientTest.class.getSimpleName() + System.nanoTime();
    }

}