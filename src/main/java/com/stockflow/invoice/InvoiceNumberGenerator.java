package com.stockflow.invoice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

/**
 * Allocates human-friendly invoice numbers such as {@code INV-2026-0001}.
 *
 * <p>The sequence row for the current year is locked pessimistically so two
 * concurrent requests cannot end up with the same number. If no row exists
 * yet we create one on demand.
 */
@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceSequenceRepository sequenceRepository;

    /**
     * REQUIRES_NEW so the lock is acquired even if the caller is already in a
     * transaction - the row is small and this avoids cascading lock waits
     * through the broader invoice creation transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextNumber(UUID ownerId) {
        int year = Year.now().getValue();
        InvoiceSequence seq = sequenceRepository.findForUpdate(ownerId, year)
            .orElseGet(() -> {
                InvoiceSequence fresh = new InvoiceSequence();
                fresh.setOwnerId(ownerId);
                fresh.setYear(year);
                fresh.setLastNumber(0L);
                return sequenceRepository.save(fresh);
            });
        seq.setLastNumber(seq.getLastNumber() + 1);
        sequenceRepository.save(seq);
        return String.format("INV-%04d-%04d", year, seq.getLastNumber());
    }
}
