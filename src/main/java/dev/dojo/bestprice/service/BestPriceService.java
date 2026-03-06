package dev.dojo.bestprice.service;

import dev.dojo.bestprice.client.ProviderClient;
import dev.dojo.bestprice.model.BestPriceResponse;
import dev.dojo.bestprice.model.ProviderPrice;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BestPriceService {

    /**
     * Comparator for finding the cheapest ProviderPrice
     */
    protected static final Comparator<BestPriceResponse> BY_PRICE =
            Comparator.comparingDouble(BestPriceResponse::price);
    private final ProviderClient providerClient;
    // Level 4: last known best price per ticker
    private final Map<String, Mono<BestPriceResponse>> cache = new ConcurrentHashMap<>();

    public BestPriceService(ProviderClient providerClient) {
        this.providerClient = providerClient;
    }

    // -------------------------------------------------------------------------
    // Provided helpers — feel free to use these
    // -------------------------------------------------------------------------

    /**
     * Returns the best (lowest) price for the given ticker across all providers.
     * <p>
     * Implement this method level by level. Each level's tests must pass before
     * moving on to the next.
     * <p>
     * ┌─ LEVEL 1 ───────────────────────────────────────────────────────────────┐
     * │ Use providerClient interface. It is already implemented.                │
     * │ You could call all 3 providers IN PARALLEL. Wait for all to respond.    │
     * │ Return the response with the lowest price.                              │
     * │                                                                         │
     * │ Hint: Flux.merge(...), collectList(), min with a Comparator             │
     * │ Trap: don't call them sequentially — that defeats the point             │
     * └─────────────────────────────────────────────────────────────────────────┘
     * <p>
     * ┌─ LEVEL 2 ──────────────────────────────────────────────────────────────┐
     * │ Apply a 600ms per-provider timeout. Drop any provider that is too slow. │
     * │ Return the best price from whoever responded in time.                   │
     * │ If NO provider responds in time, propagate the error.                   │
     * │                                                                         │
     * │ Hint: .timeout(Duration.ofMillis(600)), .onErrorResume(...)            │
     * │ Trap: applying timeout to the whole chain vs. per-provider             │
     * └─────────────────────────────────────────────────────────────────────────┘
     * <p>
     * ┌─ LEVEL 3 ──────────────────────────────────────────────────────────────┐
     * │ Gamma is flaky and returns errors. Retry it with exponential backoff.   │
     * │ The retries must not push the TOTAL response past the 600ms budget.     │
     * │                                                                         │
     * │ Hint: .retryWhen(Retry.backoff(attempts, firstBackoff))                │
     * │ Trap: the timeout and retryWhen order matters — think carefully        │
     * └─────────────────────────────────────────────────────────────────────────┘
     * <p>
     * ┌─ LEVEL 4 ──────────────────────────────────────────────────────────────┐
     * │ Cache the last known best price per ticker.                             │
     * │ If ALL providers fail (after retries/timeouts), return the cached       │
     * │ value instead of propagating an error.                                  │
     * │                                                                         │
     * │ Hint: Mono.fromCallable(() -> cache.get(ticker)), .onErrorResume(...)  │
     * │ Trap: cache must only be updated with a confirmed good response        │
     * └─────────────────────────────────────────────────────────────────────────┘
     */
    public Mono<BestPriceResponse> bestPrice(String ticker) {
        return Flux.merge(
                        providerClient.alphaPrice(ticker)
                                .timeout(Duration.ofMillis(600))
                                .onErrorResume(e -> Mono.empty()),
                        providerClient.betaPrice(ticker)
                                .timeout(Duration.ofMillis(600))
                                .onErrorResume(e -> Mono.empty()),
                        providerClient.gammaPrice(ticker)
                                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                                .timeout(Duration.ofMillis(600))
                                .onErrorResume(e -> Mono.empty())
                )
                .map(BestPriceResponse::from)
                .collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.justOrEmpty(cache.get(ticker))
                                .flatMap(mono -> mono)
                                .switchIfEmpty(Mono.error(new RuntimeException("No provider responded in time")));
                    }
                    BestPriceResponse best = list.stream().min(BY_PRICE).get();
                    Mono<BestPriceResponse> cachedMono = Mono.just(best).cache();
                    cache.put(ticker, cachedMono);
                    return cachedMono;
                });
    }
}
