package org.berk.distributedcounter.rest;

import org.berk.distributedcounter.api.Count;
import org.berk.distributedcounter.counter.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(path = "counter", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class CounterResource {

    private final Counter counter;

    public CounterResource(Counter counter) {
        this.counter = counter;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @PutMapping("/count/{id}")
    public Mono<ResponseEntity<Void>> increment(@NotEmpty(message = "Parameter 'id' is mandatory") @PathVariable("id") String counterId,
                                          @RequestParam(name = "amount", required = false) @Positive(message = "amount must be positive") Long amount) {
        log.debug("PUT /count/{} called", counterId);
        return toCancellableMono(counter.incrementAsync(counterId, amount)
                                        .thenApply(isNewRecord -> ResponseEntity.status(isNewRecord ? HttpStatus.CREATED
                                                                                                    : HttpStatus.OK).build()));
    }


    @GetMapping("/count/{id}")
    public Mono<ResponseEntity<Long>> getCounter(@NotEmpty(message = "Parameter 'id' is mandatory")
                                @PathVariable("id") String counterId) {
        return toCancellableMono(counter.getCountAsync(counterId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }


    @DeleteMapping("/count/{id}")
    public Mono<ResponseEntity<Long>> removeCounter(@NotEmpty(message = "Parameter 'id' is mandatory")
                            @PathVariable("id") String counterId) {
        log.debug("DELETE /count/{} called", counterId);
        return toCancellableMono(counter.removeAsync(counterId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @GetMapping("/list")
    public Flux<Count> listCounters(@RequestParam(name = "from_index", required = false) @Min(0) Integer fromIndex,
                                    @RequestParam(name = "item_count", required = false) @Max(500_000) @Min(1) Integer itemCount) {
        log.debug("GET /list called");
        return Flux.fromStream(counter.listCounters(fromIndex, itemCount));
    }

    @GetMapping("/listsize")
    public Mono<Integer> getSize() {
        log.debug("GET /listsize called");
        return Mono.fromCallable(counter::getSize);
    }



    private <T> Mono<T> toCancellableMono(CompletableFuture<T> completableFuture) {
        return Mono.fromFuture(completableFuture)
                .onErrorStop()
                .doOnCancel(() -> completableFuture.cancel(false));
    }
}

