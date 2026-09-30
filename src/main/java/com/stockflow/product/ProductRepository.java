package com.stockflow.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.stockflow.user.Role;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByOwnerIdAndSkuIgnoreCase(UUID ownerId, String sku);

    Optional<Product> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("""
        SELECT COUNT(p) > 0 FROM Product p
        WHERE LOWER(p.sku) = LOWER(:sku)
          AND (p.ownerId = :ownerId
               OR p.ownerId IN (SELECT u.id FROM User u WHERE u.role = :adminRole))
        """)
    boolean existsVisibleSku(@Param("ownerId") UUID ownerId,
                             @Param("sku") String sku,
                             @Param("adminRole") Role adminRole);

    @Query("""
        SELECT COUNT(p) > 0 FROM Product p
        WHERE p.id <> :excludedId
          AND LOWER(p.sku) = LOWER(:sku)
          AND (p.ownerId = :ownerId
               OR p.ownerId IN (SELECT u.id FROM User u WHERE u.role = :adminRole))
        """)
    boolean existsVisibleSkuForDifferentProduct(@Param("ownerId") UUID ownerId,
                                                @Param("sku") String sku,
                                                @Param("excludedId") UUID excludedId,
                                                @Param("adminRole") Role adminRole);

    @Query("""
        SELECT p FROM Product p
        WHERE p.id = :id
          AND (p.ownerId = :ownerId
               OR p.ownerId IN (SELECT u.id FROM User u WHERE u.role = :adminRole))
        """)
    Optional<Product> findVisibleById(@Param("ownerId") UUID ownerId,
                                      @Param("id") UUID id,
                                      @Param("adminRole") Role adminRole);

    @Query("""
        SELECT p FROM Product p
        WHERE (p.ownerId = :ownerId
               OR p.ownerId IN (SELECT u.id FROM User u WHERE u.role = :adminRole))
          AND (:q IS NULL OR :q = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Product> search(
            @Param("ownerId") UUID ownerId,
            @Param("adminRole") Role adminRole,
            @Param("q") String q,
            Pageable pageable
    );

    /**
     * Acquire an exclusive lock on the product row.
     * Used when issuing an invoice to prevent concurrent overselling.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.ownerId = :ownerId")
    Optional<Product> findForUpdate(@Param("ownerId") UUID ownerId, @Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Product p
        WHERE p.id = :id
          AND (p.ownerId = :ownerId
               OR p.ownerId IN (SELECT u.id FROM User u WHERE u.role = :adminRole))
        """)
    Optional<Product> findVisibleForUpdate(@Param("ownerId") UUID ownerId,
                                           @Param("id") UUID id,
                                           @Param("adminRole") Role adminRole);
}
