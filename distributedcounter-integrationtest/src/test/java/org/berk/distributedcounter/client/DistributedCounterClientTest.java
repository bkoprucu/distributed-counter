package org.berk.distributedcounter.client;

import org.awaitility.Awaitility;
import org.berk.distributedcounter.api.Count;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_SECOND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DistributedCounterClientTest {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    private static final DistributedCounterApacheClient client = DistributedCounterApacheClient.newClient(
            SERVER_HOST, SERVER_PORT, 250, 1000, 8);


    private final Logger log = LoggerFactory.getLogger(getClass());


    @Test
    public void shouldIncrementAndRead() {
        final int count = 10;
        String eventId = generateEventIdPrefix();
        IntStream.range(0, count).forEach(i -> client.increment(eventId));

        await().atMost(3, SECONDS).until(() -> count == client.getCount(eventId));
        assertEquals(count, client.getCount(eventId));
    }

    @Test
    public void shouldGetListAndListCount() {
        int beginWith = client.getListSize();
        final int count = 100;
        String eventIdPrefix = generateEventIdPrefix();
        List<String> eventIdList = new ArrayList<>(count);
        IntStream.range(0, count).forEach(i -> eventIdList.add(eventIdPrefix + UUID.randomUUID().toString()));

        eventIdList.forEach(client::increment);

        await().atMost(10, SECONDS).until(() -> count + beginWith == client.getListSize());
        List<Count<String>> countList = client.getCounters();
        assertEquals(count + beginWith, countList.size());

        if(beginWith == 0) {
            countList.forEach(eventCount -> {
                assertTrue(eventIdList.contains(eventCount.getId()));
                assertEquals(1L, eventCount.getCountVal().longValue());
            });
        }
    }

    @Test
    public void shouldHandleLoad() throws Exception {
        final int threads = 8;
        final int events = 10000;
        String prefix = generateEventIdPrefix();
        long duration = applyLoad(threads, events, prefix);
        log.info(() -> "Performance: " + threads * events * 1000 / duration + " requests / second\n");
        Awaitility.await().pollDelay(ONE_SECOND).pollInterval(FIVE_HUNDRED_MILLISECONDS)
                .until(() -> events == client.getCount(prefix + "_1"));
        IntStream.range(0, threads).forEach(threadId -> assertEquals(events, client.getCount(prefix + "_" + threadId)));
    }

    private long applyLoad(int threads, int events, String prefix) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        long start = System.currentTimeMillis();
        IntStream.range(0, threads).forEach(threadId ->
                    executor.submit(() -> IntStream.range(0, events).forEach(eventNo ->
                            client.increment(prefix + '_' + threadId)
                    )));
        executor.shutdown();
        executor.awaitTermination(30, SECONDS);
        return System.currentTimeMillis() - start;
    }


    private static String generateEventIdPrefix() {
        return "counter_" + System.nanoTime();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        client.close();
    }
}