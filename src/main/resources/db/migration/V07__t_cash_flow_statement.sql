-- Table: t_cash_flow_statement
-- =================================================================
CREATE TABLE public.t_cash_flow_statement (
    id VARCHAR(255) PRIMARY KEY,
    company_id INTEGER NOT NULL,
    market VARCHAR(10) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    fiscal_period VARCHAR(255) NOT NULL,
    variant VARCHAR(255) NOT NULL,
    template VARCHAR(255) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    report_date DATE NOT NULL,
    publish_date DATE NOT NULL,
    restated_date DATE,
    extracted_at TIMESTAMPTZ NOT NULL,
    statement_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_cash_flow_statement_company_id ON public.t_cash_flow_statement (company_id);


CREATE INDEX ix_cash_flow_company_date ON public.t_cash_flow_statement (company_id, report_date DESC);
