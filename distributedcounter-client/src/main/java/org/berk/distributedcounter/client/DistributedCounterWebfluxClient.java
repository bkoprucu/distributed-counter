package org.berk.distributedcounter.client;

import org.berk.distributedcounter.api.Count;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class DistributedCounterWebfluxClient {

    private final WebClient webClient;


    /**
     * Creates new client with custom {@code ClientHttpConnector}
     * @param serviceHost  Base URI of the service, e.g. "http://localhost:8080"
     * @param clientHttpConnector customized {@link ClientHttpConnector} instance
     */
    public DistributedCounterWebfluxClient(String serviceHost, ClientHttpConnector clientHttpConnector) {
        webClient = WebClient.builder().baseUrl(serviceHost.concat("/counter")).clientConnector(clientHttpConnector).build();
    }

    /**
     * Creates new client with standard {@link ReactorClientHttpConnector}
     * @param serviceHost  Base URI of the service, e.g. "http://localhost:8080"
     */
    public DistributedCounterWebfluxClient(String serviceHost) {
        this(serviceHost, new ReactorClientHttpConnector());
    }


    public Mono<Long> getCount(String countId) {
        return webClient.get().uri(uriBuilder -> uriBuilder.path("/count/{id}").build(countId))
                .retrieve().bodyToMono(Count.class).map(Count::getCountVal);
    }


    public Mono<ResponseEntity<Void>> increment(String countId) {
        return increment(countId, null);
    }


    public Mono<ResponseEntity<Void>> increment(String countId, @Nullable Integer amount) {
        return webClient.put().uri(uriBuilder -> {
            uriBuilder.path("/count/{id}");
            Optional.ofNullable(amount).ifPresent(amnt -> uriBuilder.queryParam("amount", amnt));
            return uriBuilder.build(countId);
        }).retrieve().toBodilessEntity();
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
}
