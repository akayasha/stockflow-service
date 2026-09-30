package com.stockflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Application-level settings bound to {@code stockflow.app.*}.
 */
@ConfigurationProperties(prefix = "stockflow.app")
public record AppProperties(
    BigDecimal taxRate,
    boolean seedDemoData
) {
}
