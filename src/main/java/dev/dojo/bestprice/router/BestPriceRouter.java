package dev.dojo.bestprice.router;

import dev.dojo.bestprice.handler.BestPriceHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class BestPriceRouter {

    @Bean
    public RouterFunction<ServerResponse> bestPriceRoutes(BestPriceHandler handler) {
        return RouterFunctions.route()
                .GET("/best-price/{ticker}", handler::bestPrice)
                .build();
    }
}
