-- Table: t_ticker_document
-- =================================================================
CREATE TABLE public.t_ticker_document (
    id                     VARCHAR(64)  NOT NULL,
    ticker                 VARCHAR(16)  NOT NULL,
    market                 VARCHAR(10)  NOT NULL,
    document_type          VARCHAR(20)  NOT NULL,
    document_subtype       VARCHAR(50)  NOT NULL,
    report_period          DATE         NOT NULL,
    summary                TEXT         NOT NULL,
    sentiment_score        REAL         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, report_period)
) PARTITION BY RANGE (report_period);

CREATE INDEX ix_ticker_document_lookup
    ON public.t_ticker_document (ticker, market, document_type, report_period DESC);
