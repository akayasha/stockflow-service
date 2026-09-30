package com.stockflow.invoice.statemachine;

import com.stockflow.common.error.ConflictException;
import com.stockflow.invoice.InvoiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the invoice status state machine. No Spring context,
 * no database - just the transition table.
 */
@DisplayName("InvoiceStateMachine unit tests")
class InvoiceStateMachineTest {

    @Test
    @DisplayName("DRAFT can move to ISSUED")
    void draftToIssued() {
        assertThat(InvoiceStateMachine.canTransition(InvoiceStatus.DRAFT, InvoiceStatus.ISSUED))
            .isTrue();
    }

    @Test
    @DisplayName("DRAFT can move to CANCELLED")
    void draftToCancelled() {
        assertThat(InvoiceStateMachine.canTransition(InvoiceStatus.DRAFT, InvoiceStatus.CANCELLED))
            .isTrue();
    }

    @Test
    @DisplayName("DRAFT cannot move to PAID directly")
    void draftCannotMoveToPaid() {
        assertThat(InvoiceStateMachine.canTransition(InvoiceStatus.DRAFT, InvoiceStatus.PAID))
            .isFalse();
    }

    @Test
    @DisplayName("ISSUED can move to PAID")
    void issuedToPaid() {
        assertThat(InvoiceStateMachine.canTransition(InvoiceStatus.ISSUED, InvoiceStatus.PAID))
            .isTrue();
    }

    @Test
    @DisplayName("ISSUED can move to CANCELLED")
    void issuedToCancelled() {
        assertThat(InvoiceStateMachine.canTransition(InvoiceStatus.ISSUED, InvoiceStatus.CANCELLED))
            .isTrue();
    }

    @Test
    @DisplayName("PAID is terminal - no transitions out")
    void paidIsTerminal() {
        for (InvoiceStatus target : InvoiceStatus.values()) {
            assertThat(InvoiceStateMachine.canTransition(InvoiceStatus.PAID, target))
                .as("PAID -> %s should be rejected", target)
                .isFalse();
        }
    }

    @Test
    @DisplayName("CANCELLED is terminal - no transitions out")
    void cancelledIsTerminal() {
        for (InvoiceStatus target : InvoiceStatus.values()) {
            assertThat(InvoiceStateMachine.canTransition(InvoiceStatus.CANCELLED, target))
                .as("CANCELLED -> %s should be rejected", target)
                .isFalse();
        }
    }

    @Test
    @DisplayName("A transition to the same status is rejected (no self-loops)")
    void noSelfLoops() {
        for (InvoiceStatus status : InvoiceStatus.values()) {
            assertThat(InvoiceStateMachine.canTransition(status, status))
                .as("%s -> %s should be rejected", status, status)
                .isFalse();
        }
    }

    @Test
    @DisplayName("assertTransition throws ConflictException for an illegal move")
    void assertTransitionThrowsOnIllegal() {
        InvoiceStatus from = InvoiceStatus.DRAFT;
        InvoiceStatus to = InvoiceStatus.PAID;

        assertThatThrownBy(() -> InvoiceStateMachine.assertTransition(stubInvoice(from), to))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("from DRAFT to PAID")
            .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("assertTransition is silent for a legal move")
    void assertTransitionAllowsLegal() {
        InvoiceStateMachine.assertTransition(stubInvoice(InvoiceStatus.DRAFT), InvoiceStatus.ISSUED);
    }

    /**
     * Tiny helper - we only need getStatus / getInvoiceNumber on the entity
     * for the state machine, so a Mockito-free stub is enough.
     */
    private static com.stockflow.invoice.Invoice stubInvoice(InvoiceStatus status) {
        com.stockflow.invoice.Invoice invoice = new com.stockflow.invoice.Invoice();
        invoice.setStatus(status);
        invoice.setInvoiceNumber("INV-2026-TEST");
        return invoice;
    }
}
