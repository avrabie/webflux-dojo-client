package dev.dojo.bestprice.fake;

import dev.dojo.bestprice.client.ProviderClient;
import dev.dojo.bestprice.model.ProviderPrice;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Controllable in-memory ProviderClient for unit tests.
 *
 * Default prices (used when not overridden):
 *   ALPHA → 190.00
 *   BETA  → 189.50  ← lowest by default
 *   GAMMA → 191.00
 *
 * Usage:
 *
 *   // Fixed response
 *   new FakeProviderClient().alpha(Mono.just(price("ALPHA", 195.00)))
 *
 *   // Delayed response (Level 2)
 *   new FakeProviderClient().beta(Mono.just(price("BETA", 189.50))
 *                                    .delayElement(Duration.ofMillis(900)))
 *
 *   // Fails then succeeds (Level 3 — use Mono.defer so retries re-evaluate)
 *   AtomicInteger attempts = new AtomicInteger();
 *   Mono<ProviderPrice> flaky = Mono.defer(() ->
 *       attempts.incrementAndGet() < 3
 *           ? Mono.error(new RuntimeException("503"))
 *           : Mono.just(price("GAMMA", 188.00)));
 *   new FakeProviderClient().gamma(flaky)
 *
 *   // Always fails (Level 4)
 *   new FakeProviderClient().alpha(Mono.error(new RuntimeException("down")))
 */
public class FakeProviderClient implements ProviderClient {

    private Supplier<Mono<ProviderPrice>> alpha = () -> defaultPrice("ALPHA", 190.00);
    private Supplier<Mono<ProviderPrice>> beta  = () -> defaultPrice("BETA",  189.50);
    private Supplier<Mono<ProviderPrice>> gamma = () -> defaultPrice("GAMMA", 191.00);

    /** Override the alpha response with a fixed Mono. */
    public FakeProviderClient alpha(Mono<ProviderPrice> response) {
        this.alpha = () -> response;
        return this;
    }

    /** Override the beta response with a fixed Mono. */
    public FakeProviderClient beta(Mono<ProviderPrice> response) {
        this.beta = () -> response;
        return this;
    }

    /** Override the gamma response with a fixed Mono. */
    public FakeProviderClient gamma(Mono<ProviderPrice> response) {
        this.gamma = () -> response;
        return this;
    }

    // -------------------------------------------------------------------------
    // ProviderClient implementation
    // -------------------------------------------------------------------------

    @Override
    public Mono<ProviderPrice> alphaPrice(String ticker) {
        return alpha.get();
    }

    @Override
    public Mono<ProviderPrice> betaPrice(String ticker) {
        return beta.get();
    }

    @Override
    public Mono<ProviderPrice> gammaPrice(String ticker) {
        return gamma.get();
    }

    // -------------------------------------------------------------------------
    // Factory helper
    // -------------------------------------------------------------------------

    public static ProviderPrice price(String provider, double price) {
        return new ProviderPrice("TEST", provider, price, Instant.now());
    }

    private static Mono<ProviderPrice> defaultPrice(String provider, double price) {
        return Mono.just(price(provider, price));
    }
}
