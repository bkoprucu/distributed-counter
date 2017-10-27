package org.bashar.distributedcounter.client;

import com.jayway.awaitility.Awaitility;
import org.bashar.distributedcounter.api.EventCount;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.*;


/** Integration test. Pollutes the counter by adding counters beginning with "__Test__" */
public class DistributedCounterClientTest {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

//    private DistributedCounterClient client = new DistributedCounterClient(
//            SERVER_HOST, SERVER_PORT, 250, 1000, 5);
    private DistributedCounterApacheClient client = DistributedCounterApacheClient.newClient(
            SERVER_HOST, SERVER_PORT, 250, 1000, 5);

    private static final String TEST_MARKER = "_Test_";
    @Test
    public void shouldIncrementAndRead() throws Exception {
        final int count = 10_000;
        String eventId = TEST_MARKER + UUID.randomUUID().toString();
        IntStream.range(0, count).forEach(i -> {
            client.increment(eventId);
        });
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> count == client.getCount(eventId));
        assertEquals(count, client.getCount(eventId));
    }

    @Test
    public void shouldGetListAndListCount() throws Exception {
        int beginWith = client.getListSize();
        final int count = 100;
        List<String> eventIdList = new ArrayList<>(count);
        IntStream.range(0, count).forEach(i -> eventIdList.add(TEST_MARKER + UUID.randomUUID().toString()));

        eventIdList.forEach(eventId -> client.increment(eventId));

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> count + beginWith == client.getListSize());
        List<EventCount> countList = client.getCounters();
        assertEquals(count + beginWith, countList.size());

        if(beginWith == 0) {
            countList.forEach(eventCount -> {
                assertTrue(eventIdList.contains(eventCount.getId()));
                assertEquals(1L, eventCount.getCount().longValue());
            });
        }
    }
}