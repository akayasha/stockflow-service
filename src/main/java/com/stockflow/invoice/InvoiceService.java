package com.stockflow.invoice;

import com.stockflow.common.error.ConflictException;
import com.stockflow.common.error.NotFoundException;
import com.stockflow.common.page.PageResponse;
import com.stockflow.config.AppProperties;
import com.stockflow.invoice.dto.InvoiceItemRequest;
import com.stockflow.invoice.dto.InvoiceRequest;
import com.stockflow.invoice.statemachine.InvoiceStateMachine;
import com.stockflow.product.Product;
import com.stockflow.product.ProductRepository;
import com.stockflow.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates invoice lifecycle: creation, item editing and status
 * transitions. Stock mutations live here too - that is the price we pay for
 * keeping the transactional boundary on a single service method.
 *
 * <p>All mutating methods are {@code @Transactional}. The
 * {@link ProductRepository#findForUpdate} call acquires a row-level lock on
 * the product during stock decrement, which is what keeps two concurrent
 * {@code issue} requests from overselling the same line.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final ProductRepository productRepository;
    private final StockMovementLedger stockMovementLedger;
    private final AppProperties appProperties;

    @Transactional
    public Invoice create(UUID ownerId, InvoiceRequest request) {
        Invoice invoice = new Invoice();
        invoice.setOwnerId(ownerId);
        invoice.setCustomerName(request.customerName());
        invoice.setIssueDate(request.issueDate() != null ? request.issueDate() : LocalDate.now());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());
        invoice.setStatus(InvoiceStatus.DRAFT);

        Map<UUID, Integer> requestedByProduct = aggregate(request.items());
        Map<UUID, Product> products =
                lockAndLoadProducts(ownerId, requestedByProduct.keySet());
        assertStockAvailable(products, requestedByProduct);
        int pos = 0;
        for (Map.Entry<UUID, Integer> e : requestedByProduct.entrySet()) {
            Product product = products.get(e.getKey());
            InvoiceItem item = new InvoiceItem();
            item.setProductId(product.getId());
            item.setProductNameSnapshot(product.getName());
            item.setUnitPriceSnapshot(product.getUnitPrice());
            item.setQuantity(e.getValue());
            item.setLineTotal(computeLineTotal(product.getUnitPrice(), e.getValue()));
            item.setPosition(pos++);
            invoice.addItem(item);
        }

        recomputeTotals(invoice);
        invoice.setInvoiceNumber(invoiceNumberGenerator.nextNumber(ownerId));
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created invoice {} for owner {}", saved.getInvoiceNumber(), ownerId);
        return saved;
    }

    @Transactional
    public Invoice replaceItems(UUID ownerId, UUID invoiceId, List<InvoiceItemRequest> items) {
        Invoice invoice = requireInvoice(ownerId, invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("INVOICE_NOT_EDITABLE",
                "Only DRAFT invoices may have their line items edited");
        }
        invoice.getItems().clear();
        Map<UUID, Integer> requestedByProduct = aggregate(items);
        Map<UUID, Product> products =
                lockAndLoadProducts(ownerId, requestedByProduct.keySet());
        assertStockAvailable(products, requestedByProduct);
        int pos = 0;
        for (Map.Entry<UUID, Integer> e : requestedByProduct.entrySet()) {
            Product product = products.get(e.getKey());
            InvoiceItem item = new InvoiceItem();
            item.setProductId(product.getId());
            item.setProductNameSnapshot(product.getName());
            item.setUnitPriceSnapshot(product.getUnitPrice());
            item.setQuantity(e.getValue());
            item.setLineTotal(computeLineTotal(product.getUnitPrice(), e.getValue()));
            item.setPosition(pos++);
            invoice.addItem(item);
        }
        recomputeTotals(invoice);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice issue(UUID ownerId, UUID invoiceId) {
        Invoice invoice = requireInvoice(ownerId, invoiceId);
        InvoiceStateMachine.assertTransition(invoice, InvoiceStatus.ISSUED);
        Map<UUID, Integer> requestedByProduct = aggregate(invoice);
        Map<UUID, Product> products =
                lockAndLoadProducts(ownerId, requestedByProduct.keySet());

        // Two-pass: validate stock for every line first so we never half-decrement.
        assertStockAvailable(products, requestedByProduct);
        for (Map.Entry<UUID, Integer> e : requestedByProduct.entrySet()) {
            Product p = products.get(e.getKey());
            p.setQuantityOnHand(p.getQuantityOnHand() - e.getValue());
            productRepository.save(p);
            stockMovementLedger.record(ownerId, p.getId(), invoice.getId(),
                StockMovementReason.INVOICE_ISSUED, -e.getValue());
        }
        invoice.setStatus(InvoiceStatus.ISSUED);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice markPaid(UUID ownerId, UUID invoiceId) {
        Invoice invoice = requireInvoice(ownerId, invoiceId);
        InvoiceStateMachine.assertTransition(invoice, InvoiceStatus.PAID);
        invoice.setStatus(InvoiceStatus.PAID);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice cancel(UUID ownerId, UUID invoiceId) {
        Invoice invoice = requireInvoice(ownerId, invoiceId);
        InvoiceStateMachine.assertTransition(invoice, InvoiceStatus.CANCELLED);

        if (invoice.getStatus() == InvoiceStatus.ISSUED) {
            Map<UUID, Integer> consumedByProduct = aggregate(invoice);
            Map<UUID, Product> products =
                    lockAndLoadProducts(ownerId, consumedByProduct.keySet());
            for (Map.Entry<UUID, Integer> e : consumedByProduct.entrySet()) {
                Product p = products.get(e.getKey());
                p.setQuantityOnHand(p.getQuantityOnHand() + e.getValue());
                productRepository.save(p);
                stockMovementLedger.record(ownerId, p.getId(), invoice.getId(),
                    StockMovementReason.INVOICE_CANCELLED, e.getValue());
            }
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice get(UUID ownerId, UUID id) {
        return requireInvoice(ownerId, id);
    }

    @Transactional(readOnly = true)
    public Invoice get(UUID ownerId, String role, UUID id) {
        return invoiceRepository.findVisibleById(id, ownerId, isAdmin(role))
            .orElseThrow(() -> new NotFoundException("Invoice", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<Invoice> list(UUID ownerId, InvoiceStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Invoice> result = invoiceRepository.search(ownerId, false, status, pageable);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<Invoice> list(UUID ownerId, String role, InvoiceStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Invoice> result = invoiceRepository.search(ownerId, isAdmin(role), status, pageable);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public boolean productIsReferenced(UUID productId) {
        return invoiceItemRepository.existsByProductId(productId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Invoice requireInvoice(UUID ownerId, UUID id) {
        return invoiceRepository.findByIdAndOwnerId(id, ownerId)
            .orElseThrow(() -> new NotFoundException("Invoice", id));
    }

    private boolean isAdmin(String role) {
        return Role.ADMIN.name().equalsIgnoreCase(role);
    }

    private Map<UUID, Integer> aggregate(Invoice invoice) {
        Map<UUID, Integer> map = new HashMap<>();
        for (InvoiceItem item : invoice.getItems()) {
            map.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return map;
    }

    private Map<UUID, Integer> aggregate(List<InvoiceItemRequest> items) {
        Map<UUID, Integer> map = new HashMap<>();
        for (InvoiceItemRequest req : items) {
            map.merge(req.productId(), req.quantity(), Integer::sum);
        }
        return map;
    }

    private Map<UUID, Product> lockAndLoadProducts(
            UUID ownerId,
            java.util.Set<UUID> productIds
    ) {
        Map<UUID, Product> products = new HashMap<>();

        for (UUID id : productIds) {
            Product p = productRepository.findVisibleForUpdate(ownerId, id, Role.ADMIN)
                    .orElseThrow(() -> new NotFoundException("Product", id));

            products.put(id, p);
        }

        return products;
    }

    private void assertStockAvailable(Map<UUID, Product> products, Map<UUID, Integer> requestedByProduct) {
        for (Map.Entry<UUID, Integer> e : requestedByProduct.entrySet()) {
            Product p = products.get(e.getKey());
            if (p.getQuantityOnHand() < e.getValue()) {
                throw new ConflictException("STOCK_INSUFFICIENT",
                    "Not enough stock for product '" + p.getName() + "' (have " + p.getQuantityOnHand()
                        + ", need " + e.getValue() + ")");
            }
        }
    }

    private void recomputeTotals(Invoice invoice) {
        BigDecimal subtotal = invoice.getItems().stream()
            .map(InvoiceItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxRate = appProperties.taxRate();
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        invoice.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(taxAmount);
        invoice.setTotal(total);
    }

    private BigDecimal computeLineTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
