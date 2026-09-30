package com.stockflow.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Type-safe binding for {@code stockflow.security.jwt.*}. Pulled out of the
 * config so we can mock it in tests without spinning up a Spring context.
 */
@ConfigurationProperties(prefix = "stockflow.security.jwt")
public record JwtProperties(
    String secret,
    long expirationMinutes
) {
    public Duration expiration() {
        return Duration.ofMinutes(expirationMinutes);
    }
}
