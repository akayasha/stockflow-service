package com.stockflow.invoice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Thin wrapper around {@link StockMovementRepository} that enforces the shape
 * of every stock-movement record. Lives in its own service so callers do not
 * need to know the entity type.
 */
@Service
@RequiredArgsConstructor
public class StockMovementLedger {

    private final StockMovementRepository repository;

    public void record(UUID ownerId, UUID productId, UUID invoiceId,
                       StockMovementReason reason, int delta) {
        StockMovement movement = new StockMovement();
        movement.setOwnerId(ownerId);
        movement.setProductId(productId);
        movement.setInvoiceId(invoiceId);
        movement.setReason(reason);
        movement.setDelta(delta);
        repository.save(movement);
    }
}
