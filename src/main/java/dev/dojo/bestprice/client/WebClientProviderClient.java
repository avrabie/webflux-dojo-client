package dev.dojo.bestprice.client;

import dev.dojo.bestprice.model.ProviderPrice;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Real implementation of ProviderClient — calls provider-server over HTTP.
 *
 * HTTP errors (e.g. 503 from gamma) are automatically surfaced as
 * WebClientResponseException by retrieve(), so your service can handle
 * them with onErrorResume / retryWhen without any extra plumbing here.
 */
@Component
public class WebClientProviderClient implements ProviderClient {

    private final WebClient webClient;

    public WebClientProviderClient(WebClient providerWebClient) {
        this.webClient = providerWebClient;
    }

    @Override
    public Mono<ProviderPrice> alphaPrice(String ticker) {
        return webClient.get()
                .uri("/provider/alpha/price/{ticker}", ticker)
                .retrieve()
                .bodyToMono(ProviderPrice.class);
    }

    @Override
    public Mono<ProviderPrice> betaPrice(String ticker) {
        return webClient.get()
                .uri("/provider/beta/price/{ticker}", ticker)
                .retrieve()
                .bodyToMono(ProviderPrice.class);
    }

    @Override
    public Mono<ProviderPrice> gammaPrice(String ticker) {
        return webClient.get()
                .uri("/provider/gamma/price/{ticker}", ticker)
                .retrieve()
                .bodyToMono(ProviderPrice.class);
    }
}
