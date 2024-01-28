package org.berk.distributedcounter.rest;

import jakarta.validation.constraints.Positive;
import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.counter.HazelcastCounterProperties;
import org.berk.distributedcounter.rest.api.EventCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.CREATED;


@RestController
@RequestMapping(path = "counter", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class CounterResource {

    private final Counter counter;

    private final HazelcastCounterProperties counterProperties;

    private final Environment environment;

    private static final Logger log  = LoggerFactory.getLogger(CounterResource.class);

    public CounterResource(Counter counter, Environment environment, HazelcastCounterProperties counterProperties) {
        this.counter = counter;
        this.environment = environment;
        this.counterProperties = counterProperties;
    }

    @PutMapping("/count/{eventId}")
    public Mono<ResponseEntity<Long>> increment(@PathVariable("eventId") String eventId,
                                                @RequestParam(name = "amount", required = false)
                                                @Positive(message = "amount must be positive") Integer amount,
                                                @RequestParam(name = "requestId", required = false) String requestId) {
        return counter.incrementAsync(eventId, amount, requestId)
                   .map(ResponseEntity::ok)
                   .defaultIfEmpty(ResponseEntity.status(CREATED).build());
    }


    @GetMapping("/count/{eventId}")
    public Mono<ResponseEntity<Long>> getCount(@PathVariable("eventId") String eventId) {
        return counter.getCountAsync(eventId)
                   .map(ResponseEntity::ok)
                   .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/count/{eventId}")
    public Mono<Void> deleteCount(@PathVariable("eventId") String eventId,
                                            @RequestParam(name = "requestId") String requestId) {
        return counter.remove(eventId, requestId);
    }

    @GetMapping("/size")
    public Mono<Integer> getSize() {
        return counter.getSize();
    }

    @GetMapping("/list")
    public Flux<EventCount> listCounts() {
        return counter.getCounts();
    }


    @GetMapping("/admin/info") // For testing
    public Mono<Map<String, String>> info() {
        log.debug("GET /info called");
        return Mono.fromCallable(() -> Map.of("Profiles:", List.of(environment.getActiveProfiles()).toString(),
                                 "counter properties:", counterProperties.toString(),
                                 "Pod Name", Objects.requireNonNullElse(environment.getProperty("HOSTNAME"), ""),
                                 "Pod IP", Objects.requireNonNullElse(environment.getProperty("MY_POD_IP"), "")))
                .subscribeOn(Schedulers.boundedElastic());

    }


}
