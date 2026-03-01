package dev.dojo.bestprice.client;

import dev.dojo.bestprice.model.ProviderPrice;
import reactor.core.publisher.Mono;

/**
 * Gateway to the three stock price providers.
 * You do not need to modify this interface.
 *
 * Each method returns a cold Mono — nothing happens until subscribed.
 * The real implementation uses WebClient; tests inject a fake.
 */
public interface ProviderClient {

    Mono<ProviderPrice> alphaPrice(String ticker);

    Mono<ProviderPrice> betaPrice(String ticker);

    Mono<ProviderPrice> gammaPrice(String ticker);
}
