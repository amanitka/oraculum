-- Flyway migration script for creating initial tables
-- Generated based on JPA entity definitions

-- =================================================================
-- Table: t_analysis
-- =================================================================
CREATE TABLE public.t_analysis (
    id BIGSERIAL PRIMARY KEY,
    correlation_id UUID NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    market VARCHAR(255) NOT NULL,
    analysis_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL,
    report_md TEXT,
    verdict VARCHAR(255),
    conviction INTEGER,
    payload JSONB,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ix_analysis_correlation_id ON public.t_analysis (correlation_id);
CREATE INDEX ix_analysis_ticker_market_created ON public.t_analysis (ticker, market, created_at);
CREATE INDEX ix_analysis_status_created ON public.t_analysis (status, created_at);

-- =================================================================
-- Table: t_ingestion_run_log
-- =================================================================
CREATE TABLE public.t_ingestion_run_log (
    id BIGSERIAL PRIMARY KEY,
    dataset VARCHAR(255) NOT NULL,
    run_id VARCHAR(255) NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    loaded_rows INTEGER NOT NULL,
    merged_rows INTEGER NOT NULL,
    duration_ms INTEGER NOT NULL,
    error_text TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_run_log_idempotency UNIQUE (dataset, run_id, file_checksum)
);

-- =================================================================
-- Table: t_balance_sheet
-- =================================================================
CREATE TABLE public.t_balance_sheet (
    id BIGSERIAL PRIMARY KEY,
    composite_key VARCHAR(255) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    simfin_id INTEGER NOT NULL,
    template VARCHAR(255) NOT NULL,
    variant VARCHAR(255) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    fiscal_period VARCHAR(255) NOT NULL,
    report_date DATE NOT NULL,
    publish_date DATE NOT NULL,
    restated_date DATE,
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_balance_sheet_composite_key UNIQUE (composite_key)
);

-- =================================================================
-- Table: t_cash_flow_statement
-- =================================================================
CREATE TABLE public.t_cash_flow_statement (
    id BIGSERIAL PRIMARY KEY,
    composite_key VARCHAR(255) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    simfin_id INTEGER NOT NULL,
    template VARCHAR(255) NOT NULL,
    variant VARCHAR(255) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    fiscal_period VARCHAR(255) NOT NULL,
    report_date DATE NOT NULL,
    publish_date DATE NOT NULL,
    restated_date DATE,
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_cash_flow_statement_composite_key UNIQUE (composite_key)
);

-- =================================================================
-- Table: t_income_statement
-- =================================================================
CREATE TABLE public.t_income_statement (
    id BIGSERIAL PRIMARY KEY,
    composite_key VARCHAR(255) NOT NULL,
    ticker VARCHAR(255) NOT NULL,
    simfin_id INTEGER NOT NULL,
    template VARCHAR(255) NOT NULL,
    variant VARCHAR(255) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    fiscal_period VARCHAR(255) NOT NULL,
    report_date DATE NOT NULL,
    publish_date DATE NOT NULL,
    restated_date DATE,
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_income_statement_composite_key UNIQUE (composite_key)
);

-- =================================================================
-- Table: t_industry
-- =================================================================
CREATE TABLE public.t_industry (
    industry_id VARCHAR(255) PRIMARY KEY,
    sector_name VARCHAR(255) NOT NULL,
    industry_name VARCHAR(255) NOT NULL,
    statement_template VARCHAR(255) NOT NULL,
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
-- Table: t_ticker
-- =================================================================
CREATE TABLE public.t_ticker (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255),
    provider_name VARCHAR(255),
    company_name VARCHAR(255) NOT NULL,
    industry_id VARCHAR(255),
    industry_name VARCHAR(255),
    sector_name VARCHAR(255),
    isin VARCHAR(255),
    description TEXT,
    employee_count BIGINT,
    market VARCHAR(255) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    cik VARCHAR(255),
    extracted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ticker_ticker_market UNIQUE (ticker, market)
);

-- =================================================================
-- PARTITIONED TABLES (PARENT DEFINITION)
-- =================================================================

-- Table: t_share_price (Partitioned by trade_date)
-- Note: The ID is now the primary key, and the former PK is a unique constraint.
-- =================================================================
CREATE TABLE public.t_share_price (
    id BIGSERIAL,
    ticker VARCHAR(255) NOT NULL,
    market VARCHAR(255) NOT NULL,
    trade_date DATE NOT NULL,
    sim_fin_id INTEGER,
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
    PRIMARY KEY (id, trade_date),
    CONSTRAINT uq_share_price_composite UNIQUE (ticker, market, trade_date)
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
    -- FOREIGN KEY (news_id, time_published) REFERENCES public.t_news(id, time_published) ON DELETE CASCADE -- This must be applied to partitions individually.
) PARTITION BY RANGE (time_published);

CREATE INDEX ix_news_ticker_ticker ON public.t_news_ticker (ticker);
