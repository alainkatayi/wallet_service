CREATE TABLE wallet_account (
                                id          UUID PRIMARY KEY,
                                kind        VARCHAR(10)   NOT NULL CHECK (kind IN ('USER','SYSTEM')),
                                user_id     UUID UNIQUE,
                                balance     NUMERIC(19,2) NOT NULL DEFAULT 0,
                                currency    CHAR(3)       NOT NULL DEFAULT 'CDF' CHECK (currency = 'CDF'),
                                status      VARCHAR(10)   NOT NULL CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
                                version     BIGINT        NOT NULL DEFAULT 0,
                                created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                CHECK (kind = 'SYSTEM' OR balance >= 0),
                                CHECK ((kind = 'USER' AND user_id IS NOT NULL) OR (kind = 'SYSTEM' AND user_id IS NULL))
);


CREATE TABLE ledger_entry (
                              id              UUID PRIMARY KEY,
                              account_id      UUID          NOT NULL REFERENCES wallet_account(id),
                              transaction_ref VARCHAR(64)   NOT NULL,
                              entry_type      VARCHAR(10)   NOT NULL CHECK (entry_type IN ('TOPUP','WITHDRAW','REVERSAL')),
                              direction       VARCHAR(6)    NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
                              amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                              balance_after   NUMERIC(19,2) NOT NULL,
                              created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
                              UNIQUE (account_id, transaction_ref, entry_type)
);


CREATE INDEX idx_ledger_account_date ON ledger_entry(account_id, created_at DESC);
CREATE INDEX idx_ledger_ref ON ledger_entry(transaction_ref);

CREATE TABLE wallet_limit (
                              operation_type VARCHAR(10) PRIMARY KEY CHECK (operation_type IN ('TOPUP','WITHDRAW')),
                              min_amount     NUMERIC(19,2) NOT NULL CHECK (min_amount > 0),
                              max_amount     NUMERIC(19,2) NOT NULL,
                              daily_max      NUMERIC(19,2) NOT NULL,
                              CHECK (min_amount <= max_amount AND max_amount <= daily_max)
);