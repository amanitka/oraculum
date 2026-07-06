CREATE TABLE t_ticker_document_sync_status (
    ticker                   VARCHAR(10)  NOT NULL,
    market                   VARCHAR(10)  NOT NULL DEFAULT 'US',
    source                   VARCHAR(50)  NOT NULL,         -- 'SEC_EDGAR'
    document_type            VARCHAR(20)  NOT NULL,         -- '8K', '10K'
    status                   VARCHAR(20)  NOT NULL,         -- 'COMPLETED', 'FAILED'
    extraction_status        VARCHAR(20),                   -- 'FULL', 'PARTIAL', 'EMPTY'
    message                  VARCHAR(255),                  -- Optional human-readable note
    last_processed_file_date DATE,                          -- High Water Mark sent to Python
    last_refresh_date        TIMESTAMPTZ  NOT NULL,         -- When we last asked Python
    last_file_refresh_date   TIMESTAMPTZ,                   -- When Python last found something new
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ticker, market, source, document_type)
);
