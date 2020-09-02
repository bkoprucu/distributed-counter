package org.berk.distributedcounter.rest;

import com.fasterxml.classmate.GenericType;
import org.berk.distributedcounter.api.Count;
import org.berk.distributedcounter.counter.Counter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CounterResourceTest {
    @MockBean
    Counter<String> counter;

    @Autowired
    private WebTestClient webTestClient;

    private static final GenericType<Count<String>> COUNT_STRING_TYPE = new GenericType<>(){};


    @Test
    public void increment() {
        String counterId = "abc";
        webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{counterId}").build(counterId))
        .exchange()
        .expectStatus().isNoContent();
        verify(counter).increment(counterId);
    }

    @Test
    public void get_count() {
        String counterId = "abc";
        final Count<String> count = new Count<>(counterId, 5L);
        doReturn(count).when(counter).getCount(eq(counterId));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{counterId}").build(counterId))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Count.class).isEqualTo(count);
    }


    @Test
    public void remove_counter() {
        String counterId = "abc";
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("counter/count/{counterId}").build(counterId))
                .exchange()
                .expectStatus().is2xxSuccessful();
        verify(counter).removeCounter(counterId);
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
        Count<String> firstCount = new Count<>("first", 1L);
        Count<String> secondCount = new Count<>("second", 2L);
        when(counter.listCounters(null , null)).thenReturn(Stream.of(firstCount, secondCount));
        FluxExchangeResult<Count<String>> result = webTestClient.get()
                .uri("counter/list")
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<>() { });


        List<Count<String>> counters = result.getResponseBody().collectList().block();
        assertNotNull(counters);
        assertEquals(2, counters.size());
        assertTrue(counters.containsAll(List.of(firstCount, secondCount)));
    }

    //    TODO implement global error handler and enable this
    @Test
    @Disabled
    public void validate_input() {
        Count<String> firstCount = new Count<>("first", 1L);
        Count<String> secondCount = new Count<>("second", 2L);
        when(counter.listCounters(null , null)).thenReturn(Stream.of(firstCount, secondCount));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/counter/list").queryParam("item_count", -3L).build())
                .exchange()
                .expectStatus().isBadRequest();
    }

/*
    @Test
    public void handle_errors() {
        when(counter.getCount(anyString())).thenThrow(new IllegalArgumentException());
        Response response =  target("counter/count/user1")
                .request(APPLICATION_JSON_TYPE).get();
        assertEquals(NOT_ALLOWED.getStatusCode(), response.getStatus());
    }
*/
}