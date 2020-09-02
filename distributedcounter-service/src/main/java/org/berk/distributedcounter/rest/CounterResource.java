package org.berk.distributedcounter.rest;

import org.berk.distributedcounter.api.Count;
import org.berk.distributedcounter.counter.Counter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.Optional;

@RestController
@RequestMapping(path = "counter", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class CounterResource {

    private final Counter<String> counter;


    public CounterResource(Counter<String> counter) {
        this.counter = counter;
    }

    @PutMapping("/count/{id}")
    public Mono<ResponseEntity<?>> increment(@NotEmpty(message = "Parameter 'id' is mandatory") @PathVariable("id") String counterId,
                                          @RequestParam(name = "amount", required = false) @Positive(message = "amount must be positive") Long amount) {
        return Mono.fromCallable(() -> {
            Optional.ofNullable(amount).ifPresentOrElse(amountval ->
                            counter.increment(counterId, amountval),
                    () -> counter.increment(counterId));
            return ResponseEntity.noContent().build();
        });
    }

    @GetMapping("/count/{id}")
    public Mono<Count<String>> getCount(@NotEmpty(message = "Parameter 'id' is mandatory")
                               @PathVariable("id") final String counterId) {
        return Mono.fromCallable(() -> counter.getCount(counterId));
    }

    @DeleteMapping("/count/{id}")
    public Mono<Void> removeCount(@NotEmpty(message = "Parameter 'id' is mandatory")
                            @PathVariable("id") final String counterId) {
        return Mono.fromRunnable(() -> counter.removeCounter(counterId));
    }

    @GetMapping("/list")
    public Flux<Count<String>> listCounters(@RequestParam(name = "from_index", required = false) Integer fromIndex,
                                    @RequestParam(name = "item_count", required = false) @Positive Integer itemCount) {
        return Flux.fromStream(counter.listCounters(fromIndex, itemCount));
    }

    @GetMapping("/listsize")
    public Mono<Integer> getSize() {
        return Mono.fromCallable(counter::getSize);
    }

}

