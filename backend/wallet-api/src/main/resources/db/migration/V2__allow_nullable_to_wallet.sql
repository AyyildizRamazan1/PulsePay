-- V2 — Allow to_wallet_id to be nullable
-- WITHDRAWAL transactions have no internal target wallet (money leaves the system)
ALTER TABLE transactions ALTER COLUMN to_wallet_id DROP NOT NULL;
