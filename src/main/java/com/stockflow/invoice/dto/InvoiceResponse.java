package com.stockflow.invoice.dto;

import com.stockflow.invoice.Invoice;
import com.stockflow.invoice.InvoiceItem;
import com.stockflow.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    String invoiceNumber,
    String customerName,
    LocalDate issueDate,
    LocalDate dueDate,
    InvoiceStatus status,
    String notes,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal total,
    List<InvoiceItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
    public static InvoiceResponse from(Invoice i) {
        List<InvoiceItemResponse> itemDtos = i.getItems().stream()
            .map(InvoiceItemResponse::from)
            .toList();
        return new InvoiceResponse(
            i.getId(),
            i.getInvoiceNumber(),
            i.getCustomerName(),
            i.getIssueDate(),
            i.getDueDate(),
            i.getStatus(),
            i.getNotes(),
            i.getSubtotal(),
            i.getTaxAmount(),
            i.getTotal(),
            itemDtos,
            i.getCreatedAt(),
            i.getUpdatedAt()
        );
    }
}
