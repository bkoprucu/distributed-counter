package org.berk.distributedcounter.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import org.berk.distributedcounter.api.Count;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class DistributedCounterWebfluxClientTest {

    private static WireMockServer wireMockServer;

    private DistributedCounterWebfluxClient client = new DistributedCounterWebfluxClient("http://localhost:" + wireMockServer.port());

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(wireMockConfig().asynchronousResponseEnabled(true).port(31000));
        wireMockServer.start();
        System.out.println("Wiremock server started on port " + wireMockServer.port());
    }


    @BeforeEach
    void setUp() {
        configureFor(wireMockServer.port());
    }

    @Test
    void getCount() {
        String countId = "event1";
        stubFor(get("/counter/count/" + countId).willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withBody("{\"id\": \"" + countId + "\",\"countVal\": 5}")));
        assertEquals(5, client.getCount(countId).block());
    }

    @Test
    void getListSize() {
        stubFor(get("/counter/listsize").willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withBody("4")));
        assertEquals(4, client.getListSize().block());
    }

    @Test
    void getCounters() {
        stubFor(get("/counter/list").willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withBody("[" +
                        "{\"id\": \"item1\", \"countVal\": 10}," +
                        "{\"id\": \"item2\", \"countVal\": 20}" +
                        "]")));
        List<Count> counters = client.getCounters().collectList().block();
        assertArrayEquals(counters.toArray(),  new Count[]{ new Count("item1", 10L), new Count("item2", 20L)});
    }


    @Test
    void increment() {
        String countId = "event2";
        stubFor(put("/counter/count/" + countId).willReturn(aResponse().withStatus(204)));
        assertEquals(204, client.increment(countId).block().getStatusCodeValue());
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
        System.out.println("Wiremock server stopped");
    }
}