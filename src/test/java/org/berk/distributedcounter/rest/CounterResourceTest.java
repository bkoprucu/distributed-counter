package org.berk.distributedcounter.rest;

import com.hazelcast.core.HazelcastInstance;
import org.berk.distributedcounter.Counter;
import org.berk.distributedcounter.rest.api.EventCount;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CounterResourceTest {

    @MockBean
    Counter counter;

    @Autowired
    private WebTestClient webTestClient;

    @Mock
    HazelcastInstance hazelcastInstance;

    private final String eventId = "abc";
    private final String nonExistingEventId = "non_existing";


    @Test
    public void shouldReturnHttpOkForIncrementingExistingCounter() {
        when(counter.incrementAsync(eq(eventId), isNull(), isNull()))
                .thenReturn(Mono.just(5L));
        webTestClient.put()
                     .uri(uriBuilder -> uriBuilder.path("counter/count/{eventId}").build(eventId))
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(Integer.class).isEqualTo(5);
        verify(counter).incrementAsync(eventId, null, null);
    }

    @Test
    public void shouldReturnHttpCreatedForIncrementingNonExistingCounter() {
        String requestId = "testRequestId1";
        doReturn(Mono.empty())
                .when(counter).incrementAsync(eventId, null, requestId);
        webTestClient.put()
                     .uri(uriBuilder -> uriBuilder.path("counter/count/{eventId}")
                                                  .queryParam("requestId", requestId)
                                                  .build(eventId))
                     .exchange()
                     .expectStatus().isCreated()
                     .expectBody().isEmpty();
        verify(counter).incrementAsync(eventId, null, requestId);
    }

    @Test
    public void shouldIncrementByGivenAmount() {
        String requestId = "testRequestId2";
        doReturn(Mono.empty())
                .when(counter).incrementAsync(eventId, 5, requestId);

        webTestClient.put()
                     .uri(uriBuilder -> uriBuilder.path("counter/count/{eventId}")
                                                  .queryParam("amount", "5")
                                                  .queryParam("requestId", requestId)
                                                  .build(eventId))
                     .exchange()
                     .expectStatus().isCreated()
                     .expectBody().isEmpty();
        verify(counter).incrementAsync(eventId, 5, requestId);
    }


    @Test
    public void getCountShouldReturnCount() {
        doReturn(Mono.just(5L))
                .when(counter).getCountAsync(eventId);
        webTestClient.get()
                     .uri(uriBuilder -> uriBuilder.path("counter/count/{eventId}").build(eventId))
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(Integer.class).isEqualTo(5);
        verify(counter).getCountAsync(eventId);
    }

    @Test
    public void getCountShouldReturnHttp204ForNonExistingEventId() {
        doReturn(Mono.empty())
                .when(counter).getCountAsync(nonExistingEventId);
        webTestClient.get()
                     .uri(uriBuilder -> uriBuilder.path("counter/count/{eventId}").build(nonExistingEventId))
                     .exchange()
                     .expectStatus().isNoContent()
                     .expectBody().isEmpty();
        verify(counter).getCountAsync(nonExistingEventId);
    }


    @Test
    public void shouldDeleteCounter() {
        String requestId = "testRequestId";
        webTestClient.delete()
                     .uri(uriBuilder -> uriBuilder.path("counter/count/{eventId}")
                                                  .queryParam("requestId", requestId)
                                                  .build(eventId))

                     .exchange()
                     .expectStatus().isOk()
                     .expectBody().isEmpty();
        verify(counter).remove(eventId, requestId);

    }

    @Test
    public void shouldGetSize() {
        doReturn(Mono.just(10))
                .when(counter).getSize();
        webTestClient.get()
                     .uri(uriBuilder -> uriBuilder.path("counter/size").build())
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(Integer.class).isEqualTo(10);
        verify(counter).getSize();
    }

    @Test
    public void shouldListCounters() {
        Stream<EventCount> counts = Stream.of(new EventCount("first", 10L),
                                              new EventCount("second", 20L));
        doReturn(Flux.fromStream(counts))
                .when(counter).getCounts();
        webTestClient.get()
                     .uri("counter/list")
                     .exchange()
                     .expectStatus().isOk()
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     .jsonPath("$.length()").isEqualTo(2)
                     .jsonPath("$[0].id").isEqualTo("first")
                     .jsonPath("$[0].count").isEqualTo("10")
                     .jsonPath("$[1].id").isEqualTo("second")
                     .jsonPath("$[1].count").isEqualTo("20");
        verify(counter).getCounts();
    }

}