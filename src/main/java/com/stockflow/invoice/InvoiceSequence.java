package com.stockflow.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Per-owner-per-year counter used to generate invoice numbers.
 *
 * <p>The row is locked pessimistically at generation time to make
 * {@code INV-YYYY-NNNN} allocation safe under concurrent submissions.
 * The {@code version} column is an extra belt-and-braces optimistic lock
 * for callers that update the counter through normal save semantics.
 */
@Entity
@Table(
    name = "invoice_sequences",
    uniqueConstraints = @UniqueConstraint(name = "uq_invoice_seq_owner_year", columnNames = {"owner_id", "year"})
)
@Getter
@Setter
@NoArgsConstructor
public class InvoiceSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "last_number", nullable = false)
    private long lastNumber;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
