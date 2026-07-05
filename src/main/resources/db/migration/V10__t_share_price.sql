-- Table: t_share_price (Partitioned by trade_date)
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
    PRIMARY KEY (company_id, trade_date)
) PARTITION BY RANGE (trade_date);

CREATE INDEX ix_share_price_trade_date ON public.t_share_price (trade_date);
