package com.stockflow.invoice.dto;

import com.stockflow.invoice.InvoiceItem;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
    UUID id,
    UUID productId,
    String productName,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal
) {
    public static InvoiceItemResponse from(InvoiceItem item) {
        return new InvoiceItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductNameSnapshot(),
            item.getUnitPriceSnapshot(),
            item.getQuantity(),
            item.getLineTotal()
        );
    }
}
