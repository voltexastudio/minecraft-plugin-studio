-- Bank v1 contract: per-account transaction history lookups
-- (recent activity for /bank balance and future history views).
CREATE INDEX IF NOT EXISTS idx_bank_transactions_account_id ON bank_transactions (account_id);
