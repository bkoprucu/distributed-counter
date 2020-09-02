package org.berk.distributedcounter.rest;

import org.berk.distributedcounter.counter.Counter;
import org.berk.distributedcounter.rest.api.EventCount;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.CREATED;


@RestController
@RequestMapping(path = "counter", produces = MediaType.APPLICATION_JSON_VALUE)
public class CounterResource {

    private final Counter counter;

    public CounterResource(Counter counter) {
        this.counter = counter;
    }

    @PutMapping("/count/{eventId}")
    public Mono<ResponseEntity<Long>> increment(@PathVariable("eventId") String eventId,
                                                @RequestParam(name = "amount", required = false) Integer amount,
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
    public ResponseEntity<Void> deleteCount(@PathVariable("eventId") String eventId,
                                            @RequestParam(name = "requestId") String requestId) {
        counter.remove(eventId, requestId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/size")
    public Mono<Integer> getSize() {
        return counter.getSize();
    }

    @GetMapping("/list")
    public Flux<EventCount> listCounts() {
        return counter.getCounts();
    }

}
