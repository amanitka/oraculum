-- Table: t_company_analysis
-- =================================================================
CREATE TABLE public.t_company_analysis (
    id UUID PRIMARY KEY,
    company_id INTEGER NOT NULL,
    market VARCHAR(255) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    analysis_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    report TEXT,
    outlook VARCHAR(15),
    recommendation VARCHAR(15),
    conviction INTEGER,
    analysis_data JSON,
    error TEXT,
    requested_by BIGINT REFERENCES t_user(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_company_analysis_company_id ON public.t_company_analysis (company_id);
CREATE INDEX ix_company_analysis_ticker_market_created ON public.t_company_analysis (ticker, market, created_at);
CREATE INDEX ix_company_analysis_status_created ON public.t_company_analysis (status, created_at);


