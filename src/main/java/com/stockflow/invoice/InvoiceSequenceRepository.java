package com.stockflow.invoice;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.ownerId = :ownerId AND s.year = :year")
    Optional<InvoiceSequence> findForUpdate(@Param("ownerId") UUID ownerId, @Param("year") int year);

    Optional<InvoiceSequence> findByOwnerIdAndYear(UUID ownerId, int year);
}
