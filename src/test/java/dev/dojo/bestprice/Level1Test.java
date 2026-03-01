package dev.dojo.bestprice;

import dev.dojo.bestprice.fake.FakeProviderClient;
import dev.dojo.bestprice.service.BestPriceService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static dev.dojo.bestprice.fake.FakeProviderClient.price;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Level 1 — Call all 3 providers in parallel, return the lowest price.
 *
 * All providers respond successfully. Your job: subscribe to all three
 * concurrently and return the cheapest result.
 */
class Level1Test {

    // -------------------------------------------------------------------------
    // Test 1: Beta has the lowest price — it should win
    // -------------------------------------------------------------------------
    @Test
    void returnsLowestPrice_betaWins() {
        var client = new FakeProviderClient()
                .alpha(reactor.core.publisher.Mono.just(price("ALPHA", 190.50)))
                .beta(reactor.core.publisher.Mono.just(price("BETA",  189.20)))  // lowest
                .gamma(reactor.core.publisher.Mono.just(price("GAMMA", 191.00)));

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(189.20);
                    assertThat(response.provider()).isEqualTo("BETA");
                    assertThat(response.ticker()).isEqualTo("TEST");
                })
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // Test 2: Alpha has the lowest price — it should win
    // -------------------------------------------------------------------------
    @Test
    void returnsLowestPrice_alphaWins() {
        var client = new FakeProviderClient()
                .alpha(reactor.core.publisher.Mono.just(price("ALPHA", 185.00)))  // lowest
                .beta(reactor.core.publisher.Mono.just(price("BETA",  189.50)))
                .gamma(reactor.core.publisher.Mono.just(price("GAMMA", 188.00)));

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(185.00);
                    assertThat(response.provider()).isEqualTo("ALPHA");
                })
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // Test 3: All providers return the same price — any is acceptable
    // -------------------------------------------------------------------------
    @Test
    void returnsLowestPrice_allEqual() {
        var client = new FakeProviderClient()
                .alpha(reactor.core.publisher.Mono.just(price("ALPHA", 190.00)))
                .beta(reactor.core.publisher.Mono.just(price("BETA",  190.00)))
                .gamma(reactor.core.publisher.Mono.just(price("GAMMA", 190.00)));

        var service = new BestPriceService(client);

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> assertThat(response.price()).isEqualTo(190.00))
                .verifyComplete();
    }
}
