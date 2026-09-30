package com.stockflow.invoice;

import com.stockflow.common.page.PageResponse;
import com.stockflow.common.security.AuthenticatedUser;
import com.stockflow.common.security.CurrentUser;
import com.stockflow.invoice.dto.InvoiceItemRequest;
import com.stockflow.invoice.dto.InvoiceRequest;
import com.stockflow.invoice.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice lifecycle for the current user.")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "List invoices for the current user, optionally filtered by status.")
    @GetMapping
    public PageResponse<InvoiceResponse> list(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "status", required = false) InvoiceStatus status
    ) {
        AuthenticatedUser currentUser = CurrentUser.require();
        return invoiceService.list(currentUser.id(), currentUser.role(), status, page, size)
            .map(InvoiceResponse::from);
    }

    @Operation(summary = "Get a single invoice with line items and totals.")
    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        AuthenticatedUser currentUser = CurrentUser.require();
        return InvoiceResponse.from(invoiceService.get(currentUser.id(), currentUser.role(), id));
    }

    @Operation(summary = "Create a new invoice in DRAFT status.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceRequest request) {
        return InvoiceResponse.from(invoiceService.create(CurrentUser.require().id(), request));
    }

    @Operation(summary = "Replace line items on a DRAFT invoice.")
    @PatchMapping("/{id}/items")
    public InvoiceResponse replaceItems(@PathVariable UUID id,
                                        @Valid @RequestBody List<InvoiceItemRequest> items) {
        return InvoiceResponse.from(invoiceService.replaceItems(CurrentUser.require().id(), id, items));
    }

    @Operation(summary = "Issue a DRAFT invoice. Decrements stock atomically.")
    @PostMapping("/{id}/issue")
    public InvoiceResponse issue(@PathVariable UUID id) {
        return InvoiceResponse.from(invoiceService.issue(CurrentUser.require().id(), id));
    }

    @Operation(summary = "Mark an ISSUED invoice as paid.")
    @PostMapping("/{id}/pay")
    public InvoiceResponse markPaid(@PathVariable UUID id) {
        return InvoiceResponse.from(invoiceService.markPaid(CurrentUser.require().id(), id));
    }

    @Operation(summary = "Cancel a DRAFT or ISSUED invoice. Restores stock for ISSUED ones.")
    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable UUID id) {
        return InvoiceResponse.from(invoiceService.cancel(CurrentUser.require().id(), id));
    }
}
