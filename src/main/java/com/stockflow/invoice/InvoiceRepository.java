package com.stockflow.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.id = :id
          AND (:admin = true OR i.ownerId = :ownerId)
        """)
    Optional<Invoice> findVisibleById(@Param("id") UUID id,
                                      @Param("ownerId") UUID ownerId,
                                      @Param("admin") boolean admin);

    @Query("""
        SELECT i FROM Invoice i
        WHERE (:admin = true OR i.ownerId = :ownerId)
          AND (:status IS NULL OR i.status = :status)
        """)
    Page<Invoice> search(@Param("ownerId") UUID ownerId,
                         @Param("admin") boolean admin,
                         @Param("status") InvoiceStatus status,
                         Pageable pageable);
}
