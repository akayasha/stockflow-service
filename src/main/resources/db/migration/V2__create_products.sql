-- V2: Products owned by a user. SKU uniqueness is per owner.
CREATE TABLE products (
    id                 UUID         PRIMARY KEY,
    owner_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sku                VARCHAR(64)  NOT NULL,
    name               VARCHAR(255) NOT NULL,
    description        VARCHAR(2000),
    unit_price         DECIMAL(19,2) NOT NULL CHECK (unit_price >= 0),
    quantity_on_hand   INTEGER       NOT NULL CHECK (quantity_on_hand >= 0),
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_products_owner_sku UNIQUE (owner_id, sku)
);

CREATE INDEX idx_products_owner_name ON products (owner_id, name);
