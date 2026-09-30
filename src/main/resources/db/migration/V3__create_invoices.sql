-- V3: Invoices, line items and the per-owner-per-year sequence used to
-- generate the human-readable invoice number.

CREATE TABLE invoices (
    id              UUID         PRIMARY KEY,
    owner_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invoice_number  VARCHAR(32)  NOT NULL,
    customer_name   VARCHAR(255) NOT NULL,
    issue_date      DATE         NOT NULL,
    due_date        DATE,
    status          VARCHAR(16)  NOT NULL,
    notes           VARCHAR(2000),
    subtotal        DECIMAL(19,2) NOT NULL,
    tax_amount      DECIMAL(19,2) NOT NULL,
    total           DECIMAL(19,2) NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_invoices_owner_number UNIQUE (owner_id, invoice_number)
);

CREATE INDEX idx_invoices_owner_status ON invoices (owner_id, status);
CREATE INDEX idx_invoices_owner_issue_date ON invoices (owner_id, issue_date);

CREATE TABLE invoice_items (
    id                    UUID         PRIMARY KEY,
    invoice_id            UUID         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id            UUID         NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name_snapshot VARCHAR(255) NOT NULL,
    unit_price_snapshot   DECIMAL(19,2) NOT NULL,
    quantity              INTEGER      NOT NULL CHECK (quantity > 0),
    line_total            DECIMAL(19,2) NOT NULL,
    position              INTEGER      NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),
    version               BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items (invoice_id);
CREATE INDEX idx_invoice_items_product ON invoice_items (product_id);

CREATE TABLE invoice_sequences (
    id           UUID    PRIMARY KEY,
    owner_id     UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    year         INTEGER NOT NULL,
    last_number  BIGINT  NOT NULL,
    version      BIGINT  NOT NULL DEFAULT 0,
    CONSTRAINT uq_invoice_seq_owner_year UNIQUE (owner_id, year)
);
