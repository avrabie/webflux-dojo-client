package dev.dojo.bestprice;

import dev.dojo.bestprice.fake.FakeProviderClient;
import dev.dojo.bestprice.service.BestPriceService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static dev.dojo.bestprice.fake.FakeProviderClient.price;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Level 4 — Fallback cache: serve stale data rather than an error.
 *
 * When all providers fail (after retries/timeouts), your implementation must
 * return the last known best price from a local cache instead of propagating
 * an error.
 *
 * Think carefully about:
 *   - When does the cache get populated? (only on a successful response)
 *   - What happens on the very first call if all providers fail?
 *     (no cached value → propagate the error)
 */
class Level4Test {

    // -------------------------------------------------------------------------
    // Test 1: Cache is warm — all providers fail — serve the cached value
    // -------------------------------------------------------------------------
    @Test
    void servesCachedPriceWhenAllProvidersFail() {
        var client = new FakeProviderClient(); // defaults: beta wins at 189.50
        var service = new BestPriceService(client);

        // Warm the cache with a successful call
        service.bestPrice("AAPL").block();

        // Now make every provider fail
        client.alpha(Mono.error(new RuntimeException("alpha down")))
              .beta(Mono.error(new RuntimeException("beta down")))
              .gamma(Mono.error(new RuntimeException("gamma down")));

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(189.50);  // cached best price
                    assertThat(response.provider()).isEqualTo("BETA");
                })
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // Test 2: Cache is warm — subsequent failures serve the latest cached value
    // -------------------------------------------------------------------------
    @Test
    void cacheUpdatesAfterEachSuccessfulCall() {
        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 190.00)))
                .beta(Mono.just(price("BETA",   189.50)))  // wins → cached
                .gamma(Mono.just(price("GAMMA", 191.00)));

        var service = new BestPriceService(client);

        // First successful call — caches beta at 189.50
        service.bestPrice("AAPL").block();

        // Second successful call with a different winner — cache should update
        client.alpha(Mono.just(price("ALPHA", 186.00)))  // now alpha wins
              .beta(Mono.just(price("BETA",   189.50)))
              .gamma(Mono.just(price("GAMMA", 191.00)));

        service.bestPrice("AAPL").block(); // cache now holds alpha at 186.00

        // Now fail everything — should return the updated cached value
        client.alpha(Mono.error(new RuntimeException("down")))
              .beta(Mono.error(new RuntimeException("down")))
              .gamma(Mono.error(new RuntimeException("down")));

        StepVerifier.create(service.bestPrice("AAPL"))
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(186.00);
                    assertThat(response.provider()).isEqualTo("ALPHA");
                })
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // Test 3: Cache is cold (first call) — all providers fail — must error
    // -------------------------------------------------------------------------
    @Test
    void errorsOnFirstCallWhenAllProvidersFail_noCacheYet() {
        var client = new FakeProviderClient()
                .alpha(Mono.error(new RuntimeException("alpha down")))
                .beta(Mono.error(new RuntimeException("beta down")))
                .gamma(Mono.error(new RuntimeException("gamma down")));

        var service = new BestPriceService(client); // cold cache

        StepVerifier.create(service.bestPrice("AAPL"))
                .expectError()
                .verify();
    }

    // -------------------------------------------------------------------------
    // Test 4: Cache is per-ticker — AAPL failure does not use MSFT cache
    // -------------------------------------------------------------------------
    @Test
    void cacheIsPerTicker_noTickerCrossContamination() {
        var client = new FakeProviderClient()
                .alpha(Mono.just(price("ALPHA", 380.00)))
                .beta(Mono.just(price("BETA",   375.00)))  // MSFT winner
                .gamma(Mono.just(price("GAMMA", 382.00)));

        var service = new BestPriceService(client);

        // Warm cache for MSFT
        service.bestPrice("MSFT").block();

        // Now fail all providers
        client.alpha(Mono.error(new RuntimeException("down")))
              .beta(Mono.error(new RuntimeException("down")))
              .gamma(Mono.error(new RuntimeException("down")));

        // MSFT should return from cache
        StepVerifier.create(service.bestPrice("MSFT"))
                .assertNext(r -> assertThat(r.price()).isEqualTo(375.00))
                .verifyComplete();

        // AAPL has no cache — must error
        StepVerifier.create(service.bestPrice("AAPL"))
                .expectError()
                .verify();
    }
}
