package com.stockflow.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "sku is required")
    @Size(max = 64, message = "sku must be at most 64 characters")
    String sku,

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    String name,

    @Size(max = 2000, message = "description must be at most 2000 characters")
    String description,

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "unitPrice must be >= 0")
    BigDecimal unitPrice,

    @NotNull(message = "quantityOnHand is required")
    @PositiveOrZero(message = "quantityOnHand must be >= 0")
    Integer quantityOnHand
) {
}
