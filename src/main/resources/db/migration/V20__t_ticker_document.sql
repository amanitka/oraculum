-- Table: t_ticker_document
-- =================================================================
CREATE TABLE public.t_ticker_document (
    id                     VARCHAR(64)  NOT NULL,  -- Same SHA256 hash as t_ticker_document_raw.id
    ticker                 VARCHAR(16)  NOT NULL,
    market                 VARCHAR(10)  NOT NULL DEFAULT 'US',
    document_type          VARCHAR(20)  NOT NULL,  -- '8K', '10K'
    document_subtype       VARCHAR(50)  NOT NULL,  -- 'ITEM_1A', 'ITEM_7', 'EX99_1'
    report_period          DATE         NOT NULL,  -- Partition key
    summary                TEXT         NOT NULL,  -- Subtype-specific structured JSON
    sentiment_score        REAL         NOT NULL,  -- Universal: -1.0 (bearish) to +1.0 (bullish)
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, report_period)
) PARTITION BY RANGE (report_period);

CREATE INDEX ix_ticker_document_lookup
    ON public.t_ticker_document (ticker, market, document_type, report_period DESC);
