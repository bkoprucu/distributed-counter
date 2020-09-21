package org.berk.distributedcounter.rest;

import com.fasterxml.classmate.GenericType;
import org.berk.distributedcounter.api.Count;
import org.berk.distributedcounter.counter.Counter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CounterResourceTest {
    @MockBean
    private Counter counter;

    @Autowired
    private WebTestClient webTestClient;

    private final String countId = "existing";
    private final String nonExistingCountId = "non_existing";

    @Test
    public void increment_returns_http_created_for_non_existing_countId() {
        when(counter.incrementAsync(nonExistingCountId, null)).thenReturn(CompletableFuture.supplyAsync(() -> true));
        webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{countId}").build(nonExistingCountId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();
        verify(counter).incrementAsync(nonExistingCountId, null);
    }

    @Test
    public void increment_returns_http_ok_for_existing_countId() {
        when(counter.incrementAsync(countId, null)).thenReturn(CompletableFuture.completedFuture(false));
        webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{countId}").build(countId))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
        verify(counter).incrementAsync(countId, null);
    }


    @Test
    public void get_count_should_return_http_204_for_non_existing_countId() {
        when(counter.getCountAsync(nonExistingCountId)).thenReturn(CompletableFuture.completedFuture(null));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{countId}").build(nonExistingCountId))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        verify(counter).getCountAsync(nonExistingCountId);
    }

    @Test
    public void get_count_should_return_count() {
        Long expectedCount = 5L;
        when(counter.getCountAsync(countId)).thenReturn(CompletableFuture.completedFuture(expectedCount));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{countId}").build(countId))
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(expectedCount.toString());
        verify(counter).getCountAsync(countId);
    }


    @Test
    public void remove_counter_should_return_http_204_for_non_existing_countId() {
        when(counter.removeAsync(nonExistingCountId)).thenReturn(CompletableFuture.completedFuture(null));
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{countId}").build(nonExistingCountId))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        verify(counter).removeAsync(nonExistingCountId);
    }

    @Test
    public void remove_counter_should_return_value_of_removed_item() {
        Long expectedCount = 5L;
        when(counter.removeAsync(countId)).thenReturn(CompletableFuture.completedFuture(expectedCount));
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{countId}").build(countId))
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(expectedCount.toString());
        verify(counter).removeAsync(countId);
    }

    @Test
    public void get_list_size() {
        doReturn(3).when(counter).getSize();
        webTestClient.get()
                .uri("counter/listsize")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Host")
                .expectBody(Integer.class).isEqualTo(3);
    }


    @Test
    public void list_counters() {
        Count firstCount = new Count("first", 1L);
        Count secondCount = new Count("second", 2L);
        when(counter.listCounters(null , null)).thenReturn(Stream.of(firstCount, secondCount));
        FluxExchangeResult<Count> result = webTestClient.get()
                .uri("counter/list")
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<>() { });


        List<Count> counters = result.getResponseBody().collectList().block();
        assertNotNull(counters);
        assertEquals(2, counters.size());
        assertTrue(counters.containsAll(List.of(firstCount, secondCount)));
    }

    @Test
    public void invalid_input_should_respond_with_http_400() {
        Count firstCount = new Count("first", 1L);
        Count secondCount = new Count("second", 2L);
        when(counter.listCounters(null , null)).thenReturn(Stream.of(firstCount, secondCount));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/counter/list").queryParam("item_count", -3L).build())
                .exchange()
                .expectStatus().isBadRequest();
    }


}