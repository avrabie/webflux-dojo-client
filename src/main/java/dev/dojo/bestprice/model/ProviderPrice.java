package dev.dojo.bestprice.model;

import java.time.Instant;

/**
 * Price response received from a provider endpoint.
 * Maps directly to the JSON returned by provider-server.
 */
public record ProviderPrice(
        String ticker,
        String provider,
        double price,
        Instant timestamp
) {}
