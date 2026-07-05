-- Table: t_market
-- =================================================================
CREATE TABLE public.t_market (
    market_id VARCHAR(255) PRIMARY KEY,
    market_name VARCHAR(255) NOT NULL,
    currency VARCHAR(255),
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- =================================================================
-- PARTITIONED TABLES (PARENT DEFINITION)
-- =================================================================

-- Table: t_share_price (Partitioned by trade_date)
-- =================================================================
CREATE TABLE public.t_share_price (
    company_id INTEGER NOT NULL,
    trade_date DATE NOT NULL,
    market VARCHAR(10) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    currency VARCHAR(255),
    open REAL,
    high REAL,
    low REAL,
    close REAL,
    adj_close REAL,
    volume BIGINT,
    shares_outstanding BIGINT,
    dividend REAL,
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_id, trade_date),
    FOREIGN KEY (company_id) REFERENCES public.t_company(id)
) PARTITION BY RANGE (trade_date);

CREATE INDEX ix_share_price_trade_date ON public.t_share_price (trade_date);

