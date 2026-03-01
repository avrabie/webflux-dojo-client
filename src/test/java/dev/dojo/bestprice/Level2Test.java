package dev.dojo.bestprice;

import dev.dojo.bestprice.fake.FakeProviderClient;
import dev.dojo.bestprice.service.BestPriceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static dev.dojo.bestprice.fake.FakeProviderClient.price;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Level 2 — Apply a 600ms per-provider budget. Drop slow providers.
 *
 * Beta responds in 900ms — well past the 600ms budget.
 * Your implementation must drop beta and return the best of the remaining two.
 *
 * These tests use real time. They will take ~300ms to run — that's expected.
 */
class Level2Test {

    // -------------------------------------------------------------------------
    // Test 1: Beta is too slow — gamma wins with the lowest in-time price
    // -------------------------------------------------------------------------
    @Test
    void dropsSlowProvider_gammaWins() {
        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 190.50)).delayElement(Duration.ofMillis(200)))
                .beta(Mono.just(price("BETA",   189.20)).delayElement(Duration.ofMillis(900))) // too slow
                .gamma(Mono.just(price("GAMMA", 190.00)).delayElement(Duration.ofMillis(300)));

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(190.00);
                    assertThat(response.provider()).isEqualTo("GAMMA");
                })
                // must finish before beta's 900ms delay would have elapsed
                .expectComplete()
                .verify(Duration.ofMillis(800));
    }

    // -------------------------------------------------------------------------
    // Test 2: Alpha is too slow — beta and gamma race, gamma wins
    // -------------------------------------------------------------------------
    @Test
    void dropsSlowProvider_betaWins() {
        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 185.00)).delayElement(Duration.ofMillis(800))) // too slow
                .beta(Mono.just(price("BETA",   189.50)).delayElement(Duration.ofMillis(500)))
                .gamma(Mono.just(price("GAMMA", 190.00)).delayElement(Duration.ofMillis(300)));

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(189.50);
                    assertThat(response.provider()).isEqualTo("BETA");
                })
                .expectComplete()
                .verify(Duration.ofMillis(750));
    }

    // -------------------------------------------------------------------------
    // Test 3: All providers are too slow — the pipeline should error
    // -------------------------------------------------------------------------
    @Test
    void errorsWhenAllProvidersAreTooSlow() {
        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 190.00)).delayElement(Duration.ofMillis(700)))
                .beta(Mono.just(price("BETA",   189.50)).delayElement(Duration.ofMillis(800)))
                .gamma(Mono.just(price("GAMMA", 191.00)).delayElement(Duration.ofMillis(900)));

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .expectError()
                .verify(Duration.ofMillis(1500));
    }
}
