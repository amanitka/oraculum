-- Flyway migration script for creating initial tables
-- Schema updated based on migration guide

-- =================================================================
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
    currency VARCHAR(255) NOT NULL,
    cik VARCHAR(255),
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_company_ticker_market UNIQUE (ticker, market)
);

-- =================================================================
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
    analysis_data JSONB,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (company_id) REFERENCES public.t_company(id)
);

CREATE INDEX ix_company_analysis_company_id ON public.t_company_analysis (company_id);
CREATE INDEX ix_company_analysis_ticker_market_created ON public.t_company_analysis (ticker, market, created_at);
CREATE INDEX ix_company_analysis_status_created ON public.t_company_analysis (status, created_at);


-- =================================================================
-- Table: t_load_log
-- =================================================================
CREATE TABLE public.t_load_log (
    id BIGSERIAL PRIMARY KEY,
    dataset VARCHAR(255) NOT NULL,
    run_id VARCHAR(255) NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    loaded_rows INTEGER NOT NULL,
    merged_rows INTEGER NOT NULL,
    error_text TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_load_log_idempotency UNIQUE (dataset, run_id, file_checksum)
);

-- =================================================================
-- Table: t_balance_sheet
-- =================================================================
CREATE TABLE public.t_balance_sheet (
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
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (company_id) REFERENCES public.t_company(id)
);

CREATE INDEX ix_balance_sheet_company_id ON public.t_balance_sheet (company_id);

-- =================================================================
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
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (company_id) REFERENCES public.t_company(id)
);

CREATE INDEX ix_cash_flow_statement_company_id ON public.t_cash_flow_statement (company_id);

-- =================================================================
-- Table: t_income_statement
-- =================================================================
CREATE TABLE public.t_income_statement (
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
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (company_id) REFERENCES public.t_company(id)
);

CREATE INDEX ix_income_statement_company_id ON public.t_income_statement (company_id);

-- =================================================================
-- Table: t_industry
-- =================================================================
CREATE TABLE public.t_industry (
    industry_id VARCHAR(255) PRIMARY KEY,
    sector_name VARCHAR(255) NOT NULL,
    industry_name VARCHAR(255) NOT NULL,
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- =================================================================
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

-- =================================================================
-- Table: t_news (Partitioned by time_published)
-- =================================================================
CREATE TABLE public.t_news (
    id VARCHAR(64) NOT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    time_published TIMESTAMPTZ NOT NULL,
    authors JSONB,
    summary TEXT NOT NULL,
    source VARCHAR(255),
    category_within_source VARCHAR(255),
    source_domain VARCHAR(255),
    topics JSONB,
    overall_sentiment_score REAL,
    overall_sentiment_label VARCHAR(50),
    extracted_at TIMESTAMPTZ NOT NULL,
    sentiment_score_definition TEXT,
    relevance_score_definition TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, time_published)
) PARTITION BY RANGE (time_published);

CREATE INDEX ix_news_time_published ON public.t_news (time_published);

-- =================================================================
-- Table: t_news_ticker (Partitioned by time_published)
-- =================================================================
CREATE TABLE public.t_news_ticker (
    news_id VARCHAR(64) NOT NULL,
    ticker VARCHAR(16) NOT NULL,
    time_published TIMESTAMPTZ NOT NULL,
    relevance_score REAL,
    ticker_sentiment_score REAL,
    ticker_sentiment_label VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (news_id, ticker, time_published)
) PARTITION BY RANGE (time_published);

CREATE INDEX ix_news_ticker_ticker ON public.t_news_ticker (ticker);