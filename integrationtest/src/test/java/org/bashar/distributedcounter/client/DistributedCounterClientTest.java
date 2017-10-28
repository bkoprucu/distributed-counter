package org.bashar.distributedcounter.client;

import org.bashar.distributedcounter.api.EventCount;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;



public class DistributedCounterClientTest {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    private DistributedCounterApacheClient client = DistributedCounterApacheClient.newClient(
            SERVER_HOST, SERVER_PORT, 250, 1000, 12);

    @Test
    public void shouldIncrementAndRead() throws Exception {
        final int count = 100;
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
        final int threads = 12;
        final int events = 10000;

        String prefix = generateEventIdPrefix();
        // Warmup
        applyLoad(threads, events, prefix);

        long duration = applyLoad(threads, events, prefix);
        System.out.println("\nPerformance: " + threads * events * 1000 / duration + " requests / second\n");

    }

    private long applyLoad(int threads, int events, String prefix) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        long start = System.currentTimeMillis();
        IntStream.range(0, threads).forEach(threadId ->
                executor.submit(() -> IntStream.range(0, events).forEach(eventNo -> client.increment(prefix + threadId)))
        );
        executor.shutdown();
        executor.awaitTermination(30, SECONDS);
        return System.currentTimeMillis() - start;
    }


    private static String generateEventIdPrefix() {
        return "_test_" + DistributedCounterClientTest.class.getSimpleName() + System.nanoTime();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }
}