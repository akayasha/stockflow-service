-- V4: Append-only stock movement ledger.
CREATE TABLE stock_movements (
    id          UUID         PRIMARY KEY,
    owner_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    invoice_id  UUID         REFERENCES invoices(id) ON DELETE SET NULL,
    reason      VARCHAR(32)  NOT NULL,
    delta       INTEGER      NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_stock_movements_owner_product ON stock_movements (owner_id, product_id);
CREATE INDEX idx_stock_movements_invoice ON stock_movements (invoice_id);
