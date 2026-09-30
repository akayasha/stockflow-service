package com.stockflow.invoice.statemachine;

import com.stockflow.common.error.ConflictException;
import com.stockflow.invoice.Invoice;
import com.stockflow.invoice.InvoiceStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure state-machine helper for invoice status transitions.
 *
 * <p>Keeps the rules in one place so {@link InvoiceService} does not need to
 * know whether {@code PAID} is reachable from {@code DRAFT} - it just asks
 * "can I move here?".
 */
public final class InvoiceStateMachine {

    private static final Map<InvoiceStatus, Set<InvoiceStatus>> ALLOWED = Map.of(
        InvoiceStatus.DRAFT,     EnumSet.of(InvoiceStatus.ISSUED, InvoiceStatus.CANCELLED),
        InvoiceStatus.ISSUED,    EnumSet.of(InvoiceStatus.PAID, InvoiceStatus.CANCELLED),
        InvoiceStatus.PAID,      EnumSet.noneOf(InvoiceStatus.class),
        InvoiceStatus.CANCELLED, EnumSet.noneOf(InvoiceStatus.class)
    );

    private InvoiceStateMachine() {
    }

    public static boolean canTransition(InvoiceStatus from, InvoiceStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(InvoiceStatus.class)).contains(to);
    }

    public static void assertTransition(Invoice invoice, InvoiceStatus target) {
        if (!canTransition(invoice.getStatus(), target)) {
            throw new ConflictException(
                "INVALID_STATUS_TRANSITION",
                "Cannot move invoice " + invoice.getInvoiceNumber() + " from " + invoice.getStatus() + " to " + target);
        }
    }
}
