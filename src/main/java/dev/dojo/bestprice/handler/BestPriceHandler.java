package dev.dojo.bestprice.handler;

import dev.dojo.bestprice.service.BestPriceService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class BestPriceHandler {

    private final BestPriceService bestPriceService;

    public BestPriceHandler(BestPriceService bestPriceService) {
        this.bestPriceService = bestPriceService;
    }

    public Mono<ServerResponse> bestPrice(ServerRequest request) {
        String ticker = request.pathVariable("ticker");
        return bestPriceService.bestPrice(ticker)
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }
}
