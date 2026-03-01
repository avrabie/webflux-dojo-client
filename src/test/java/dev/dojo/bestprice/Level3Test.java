package dev.dojo.bestprice;

import dev.dojo.bestprice.fake.FakeProviderClient;
import dev.dojo.bestprice.service.BestPriceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.dojo.bestprice.fake.FakeProviderClient.price;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Level 3 — Retry gamma on failure, but respect the 600ms total budget.
 *
 * Gamma fails with an error before eventually succeeding.
 * Your implementation must retry gamma (with backoff), while ensuring the
 * total response time stays within the 600ms budget.
 *
 * Key question to ask yourself: does the timeout wrap the retry, or does
 * the retry wrap the timeout? The order matters.
 */
class Level3Test {

    // -------------------------------------------------------------------------
    // Test 1: Gamma fails once then succeeds — gamma wins overall
    // -------------------------------------------------------------------------
    @Test
    void retriesGammaOnFailure_gammaUltimatelyWins() {
        var attempts = new AtomicInteger(0);

        // Mono.defer ensures the lambda re-evaluates on each subscription (i.e. each retry)
        Mono<dev.dojo.bestprice.model.ProviderPrice> flakyGamma = Mono.defer(() ->
                attempts.incrementAndGet() < 2
                        ? Mono.error(new RuntimeException("503 Service Unavailable"))
                        : Mono.just(price("GAMMA", 187.00))  // succeeds on 2nd attempt
        );

        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 190.50)))
                .beta(Mono.just(price("BETA",   189.80)))
                .gamma(flakyGamma);

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(187.00);
                    assertThat(response.provider()).isEqualTo("GAMMA");
                })
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // Test 2: Gamma keeps failing — falls back to alpha/beta within the budget
    // -------------------------------------------------------------------------
    @Test
    void retriesGammaExhausted_fallsBackToBestAvailable() {
        // Gamma always fails — retries are exhausted, gamma is dropped
        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 190.50)))
                .beta(Mono.just(price("BETA",   189.80)))  // wins after gamma is dropped
                .gamma(Mono.error(new RuntimeException("gamma permanently down")));

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(189.80);
                    assertThat(response.provider()).isEqualTo("BETA");
                })
                .expectComplete()
                .verify(Duration.ofMillis(1500)); // retries add some time — still bounded
    }

    // -------------------------------------------------------------------------
    // Test 3: Gamma fails and retries push past the budget — gamma must be dropped
    // -------------------------------------------------------------------------
    @Test
    void gammaRetriesExceedBudget_gammaIsDropped() {
        // Each gamma attempt takes 250ms, so 3 attempts = 750ms > 600ms budget
        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 192.00)).delayElement(Duration.ofMillis(100)))
                .beta(Mono.just(price("BETA",   191.00)).delayElement(Duration.ofMillis(200))) // wins
                .gamma(Mono.just(price("GAMMA", 188.00))
                        .delayElement(Duration.ofMillis(250))
                        .flatMap(_ -> Mono.error(new RuntimeException("503")))); // always fails slowly

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    // gamma's 188.00 should NOT win — it was dropped due to budget
                    assertThat(response.price()).isGreaterThan(188.00);
                    assertThat(response.provider()).isNotEqualTo("GAMMA");
                })
                .expectComplete()
                .verify(Duration.ofMillis(900));
    }
}
