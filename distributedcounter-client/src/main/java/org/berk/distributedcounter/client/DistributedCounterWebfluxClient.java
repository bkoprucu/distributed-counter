package org.berk.distributedcounter.client;

import org.berk.distributedcounter.api.Count;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class DistributedCounterWebfluxClient {

    private final WebClient webClient;
    private final String baseUrl;

    /**
     * Creates new client with custom {@code ClientHttpConnector}
     * @param serviceHost  Base URI of the service, e.g. "http://localhost:8080"
     * @param clientHttpConnector customized {@link ClientHttpConnector} instance
     */
    public DistributedCounterWebfluxClient(String serviceHost, ClientHttpConnector clientHttpConnector) {
        baseUrl = serviceHost.concat("/counter");
        webClient = WebClient.builder().baseUrl(baseUrl).clientConnector(clientHttpConnector).build();
    }

    /**
     * Creates new client with standard {@link ReactorClientHttpConnector}
     * @param serviceHost  Base URI of the service, e.g. "http://localhost:8080"
     */
    public DistributedCounterWebfluxClient(String serviceHost) {
        this(serviceHost, new ReactorClientHttpConnector());
    }


    /**
     * Get count
     * @return  count, or null if a counter with countId doesn't exist
     */
    public Mono<Long> getCount(String countId) {
        return webClient.get().uri(uriBuilder -> uriBuilder.path("/count/{id}").build(countId))
                .retrieve().bodyToMono(Long.class);
    }


    /**
     * Incremet counter countId by one
     * @return True if a new counter has been created, false otherwise.
     */
    public Mono<Boolean> increment(String countId) {
        return increment(countId, null);
    }


    /**
     * Incremet counter countId by {@code amount}
     * @return True if a new counter has been created, false otherwise.
     */
    public Mono<Boolean> increment(String countId, @Nullable Integer amount) {
        return webClient.put().uri(uriBuilder -> {
            uriBuilder.path("/count/{id}");
            Optional.ofNullable(amount).ifPresent(amnt -> uriBuilder.queryParam("amount", amnt));
            return uriBuilder.build(countId);
        }).retrieve()
                       .toBodilessEntity()
                       .map(responseEntity -> responseEntity.getStatusCode() == HttpStatus.CREATED);
    }


    public Flux<Count> getCounters() {
        return getCounters(null, null);
    }


    public Flux<Count> getCounters(@Nullable Integer fromIndex, @Nullable Integer itemCount) {
        return webClient.get().uri(uriBuilder -> {
            uriBuilder.path("/list");
            Optional.ofNullable(fromIndex).ifPresent(fr -> uriBuilder.queryParam("from_index", fr));
            Optional.ofNullable(itemCount).ifPresent(itc -> uriBuilder.queryParam("item_count", itc));
            return uriBuilder.build();
        }).retrieve().bodyToFlux(Count.class);
    }


    public Mono<Integer> getListSize() {
        return webClient.get().uri("/listsize").retrieve().bodyToMono(Integer.class);
    }


    public Mono<Long> removeCounter(String countId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/count/{id}").build(countId))
                .retrieve()
                .bodyToMono(Long.class);
    }


    public String getBaseUrl() {
        return baseUrl;
    }

    public WebClient getWebClient() {
        return webClient;
    }
}
