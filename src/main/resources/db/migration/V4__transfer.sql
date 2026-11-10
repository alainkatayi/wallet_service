ALTER TABLE ledger_entry DROP CONSTRAINT IF EXISTS ledger_entry_entry_type_check;
ALTER TABLE ledger_entry
    ADD CONSTRAINT ledger_entry_entry_type_check
        CHECK (entry_type IN ('TOPUP', 'WITHDRAW', 'REVERSAL', 'TRANSFER'));

ALTER TABLE wallet_limit DROP CONSTRAINT IF EXISTS wallet_limit_operation_type_check;
ALTER TABLE wallet_limit
    ADD CONSTRAINT wallet_limit_operation_type_check
        CHECK (operation_type IN ('TOPUP', 'WITHDRAW', 'TRANSFER'));

INSERT INTO wallet_limit (operation_type, min_amount, max_amount, daily_max)
VALUES ('TRANSFER', 500.00, 2000000.00, 5000000.00)
ON CONFLICT (operation_type) DO NOTHING;
