-- Table: t_company
-- =================================================================
CREATE TABLE public.t_company (
    id INTEGER PRIMARY KEY,
    market VARCHAR(10) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    industry_id VARCHAR(255),
    industry_name VARCHAR(255),
    sector_name VARCHAR(255),
    isin VARCHAR(255),
    description TEXT,
    employee_count BIGINT,
    currency VARCHAR(255),
    cik VARCHAR(255),
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_company_ticker_market UNIQUE (ticker, market)
);

