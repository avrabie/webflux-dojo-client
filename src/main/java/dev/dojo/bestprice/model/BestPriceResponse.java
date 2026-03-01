package dev.dojo.bestprice.model;

import java.time.Instant;

/**
 * The response your API returns: the best (lowest) price found across all providers.
 */
public record BestPriceResponse(
        String ticker,
        String provider,
        double price,
        Instant timestamp
) {
    public static BestPriceResponse from(ProviderPrice p) {
        return new BestPriceResponse(p.ticker(), p.provider(), p.price(), p.timestamp());
    }
}
