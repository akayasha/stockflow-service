package com.stockflow.invoice.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record InvoiceItemRequest(
    @NotNull(message = "productId is required")
    UUID productId,

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than zero")
    Integer quantity
) {
}
