package com.stockflow.product;

import com.stockflow.common.error.ConflictException;
import com.stockflow.common.error.NotFoundException;
import com.stockflow.common.page.PageResponse;
import com.stockflow.invoice.InvoiceService;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public Product create(UUID ownerId, ProductRequest req) {
        if (productRepository.existsVisibleSku(ownerId, req.sku(), Role.ADMIN)) {
            throw new ConflictException(
                    "SKU_TAKEN",
                    "A product with SKU '" + req.sku() + "' already exists"
            );
        }

        Product p = new Product();
        p.setOwnerId(ownerId);
        p.setSku(req.sku());
        p.setName(req.name());
        p.setDescription(req.description());
        p.setUnitPrice(req.unitPrice());
        p.setQuantityOnHand(req.quantityOnHand());

        return productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Product get(UUID ownerId, UUID id) {
        return productRepository.findVisibleById(ownerId, id, Role.ADMIN)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<Product> list(UUID ownerId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("name").ascending()
        );

        Page<Product> result = productRepository.search(ownerId, Role.ADMIN, query, pageable);

        return PageResponse.of(result);
    }

    @Transactional
    public Product update(UUID ownerId, UUID id, ProductRequest req) {
        Product p = productRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Product", id));

        if (!p.getSku().equalsIgnoreCase(req.sku())
                && productRepository.existsVisibleSkuForDifferentProduct(
                    ownerId, req.sku(), p.getId(), Role.ADMIN)) {

            throw new ConflictException(
                    "SKU_TAKEN",
                    "A product with SKU '" + req.sku() + "' already exists"
            );
        }

        p.setSku(req.sku());
        p.setName(req.name());
        p.setDescription(req.description());
        p.setUnitPrice(req.unitPrice());
        p.setQuantityOnHand(req.quantityOnHand());

        return productRepository.save(p);
    }

    @Transactional
    public void delete(UUID ownerId, UUID id) {
        Product p = productRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Product", id));

        if (invoiceService.productIsReferenced(p.getId())) {
            throw new ConflictException(
                    "PRODUCT_REFERENCED",
                    "Product '" + p.getName()
                            + "' is referenced by at least one invoice and cannot be deleted"
            );
        }

        productRepository.delete(p);
    }
}
