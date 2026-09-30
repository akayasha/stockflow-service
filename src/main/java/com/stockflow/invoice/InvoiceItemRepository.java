package com.stockflow.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    boolean existsByProductId(UUID productId);

    long countByProductId(UUID productId);
}
