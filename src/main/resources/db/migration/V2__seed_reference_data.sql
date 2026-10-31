-- Compte système unique (contrepartie de toutes les écritures du ledger)
INSERT INTO wallet_account (id, kind, status)
VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM', 'ACTIVE');

-- Limites par défaut en CDF (modifiables plus tard par une nouvelle migration)
INSERT INTO wallet_limit (operation_type, min_amount, max_amount, daily_max) VALUES
    ('TOPUP',    500.00, 2000000.00, 5000000.00),
    ('WITHDRAW', 500.00, 2000000.00, 5000000.00);