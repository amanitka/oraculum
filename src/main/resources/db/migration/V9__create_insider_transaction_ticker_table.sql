CREATE TABLE t_insider_transaction_ticker (
    id VARCHAR(64),
    ticker VARCHAR(10) NOT NULL,
    insider_name VARCHAR(255),
    title VARCHAR(255),
    trade_type VARCHAR(50),
    currency VARCHAR(3) DEFAULT 'USD',
    price NUMERIC,
    qty NUMERIC,
    owned NUMERIC,
    delta_own NUMERIC,
    value NUMERIC,
    filing_date TIMESTAMP,
    trade_date DATE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, filing_date)
) PARTITION BY RANGE (filing_date);

CREATE INDEX idx_insider_transaction_ticker ON t_insider_transaction_ticker (ticker);
CREATE INDEX idx_insider_transaction_filing_date ON t_insider_transaction_ticker (filing_date);

DO $$ 
BEGIN 
    PERFORM create_yearly_partitions('t_insider_transaction_ticker', (NOW() - INTERVAL '3 years')::DATE, (NOW() + INTERVAL '1 years')::DATE);
END $$;
