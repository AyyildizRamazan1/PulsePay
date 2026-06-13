-- =============================================================================
-- V1 — Initial Schema
-- PulsePay Wallet API
-- =============================================================================

-- -----------------------------------------------------------------------
-- USERS
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    phone      VARCHAR(20),
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------
-- WALLETS
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallets (
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        NOT NULL UNIQUE,
    balance    DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    version    BIGINT        NOT NULL DEFAULT 0,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user   FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_wallet_balance CHECK (balance >= 0)
);

CREATE INDEX idx_wallet_user_id ON wallets(user_id);

-- -----------------------------------------------------------------------
-- TRANSACTIONS
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL     PRIMARY KEY,
    from_wallet_id  BIGINT,
    to_wallet_id    BIGINT        NOT NULL,
    amount          DECIMAL(19,2) NOT NULL,
    type            VARCHAR(50)   NOT NULL,
    status          VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(255)  UNIQUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP,
    error_message   VARCHAR(500),
    CONSTRAINT fk_tx_from_wallet FOREIGN KEY (from_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_tx_to_wallet   FOREIGN KEY (to_wallet_id)   REFERENCES wallets(id),
    CONSTRAINT chk_tx_amount     CHECK (amount > 0)
);

CREATE INDEX idx_tx_from_wallet_id  ON transactions(from_wallet_id);
CREATE INDEX idx_tx_to_wallet_id    ON transactions(to_wallet_id);
CREATE INDEX idx_tx_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_tx_status          ON transactions(status);
CREATE INDEX idx_tx_created_at      ON transactions(created_at);

-- -----------------------------------------------------------------------
-- AUDIT_LOGS
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGSERIAL     PRIMARY KEY,
    wallet_id      BIGINT,
    transaction_id BIGINT,
    user_id        BIGINT,
    action         VARCHAR(100)  NOT NULL,
    amount         DECIMAL(19,2),
    ip_address     VARCHAR(45),
    status         VARCHAR(50),
    error_message  VARCHAR(500),
    timestamp      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_audit_wallet_id ON audit_logs(wallet_id);
CREATE INDEX idx_audit_user_id   ON audit_logs(user_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);