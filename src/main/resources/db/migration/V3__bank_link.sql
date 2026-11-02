-- Numéro de compte bancaire lié. NULL multiples autorisés : le compte SYSTEM reste sans numéro.
ALTER TABLE wallet_account
    ADD COLUMN bank_account_number CHAR(12);

ALTER TABLE wallet_account
    ADD CONSTRAINT wallet_account_bank_account_number_key UNIQUE (bank_account_number);

CREATE TABLE wallet_link_attempt (
    link_id        UUID PRIMARY KEY,
    user_id        UUID        NOT NULL,
    account_number CHAR(12)    NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallet_link_attempt_user_created
    ON wallet_link_attempt (user_id, created_at DESC);
