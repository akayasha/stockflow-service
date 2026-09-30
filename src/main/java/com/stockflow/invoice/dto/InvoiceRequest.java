package com.stockflow.invoice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
    @NotBlank(message = "customerName is required")
    @Size(max = 255, message = "customerName must be at most 255 characters")
    String customerName,

    LocalDate issueDate,
    LocalDate dueDate,

    @Size(max = 2000, message = "notes must be at most 2000 characters")
    String notes,

    @NotEmpty(message = "at least one line item is required")
    List<@Valid InvoiceItemRequest> items
) {

    @JsonIgnore
    @AssertTrue(message = "dueDate must not be before issueDate")
    public boolean isDueDateValid() {
        if (dueDate == null) return true;
        // issueDate kosong berarti service memakai hari ini
        LocalDate effectiveIssueDate = issueDate != null ? issueDate : LocalDate.now();
        return !dueDate.isBefore(effectiveIssueDate);
    }
}
