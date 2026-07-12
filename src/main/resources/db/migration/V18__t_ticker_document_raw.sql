CREATE TABLE t_ticker_document_raw (
    id               VARCHAR(64)  NOT NULL, -- SHA256 Hash
    ticker           VARCHAR(20)  NOT NULL,
    market           VARCHAR(10)  NOT NULL DEFAULT 'US',
    source           VARCHAR(50)  NOT NULL, -- 'SEC_EDGAR'
    document_type    VARCHAR(20)  NOT NULL, -- '8K', '10K'
    document_subtype VARCHAR(50)  NOT NULL, -- 'EX99_1', 'ITEM_1A', 'ITEM_7'
    accession_number VARCHAR(50),           -- SEC filing unique ID
    source_url       VARCHAR(2048),         -- Link to original document
    report_period    DATE         NOT NULL, -- Used for Partitioning (e.g., Fiscal Quarter End)
    filing_date      DATE         NOT NULL,
    content          TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    extracted_at     TIMESTAMPTZ  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, report_period)
) PARTITION BY RANGE (report_period);

-- Fast lookup for the LLM processing queue
CREATE INDEX ix_ticker_document_raw_queue
    ON t_ticker_document_raw(ticker, market, source, document_type, document_subtype, report_period)
    WHERE status = 'PENDING';
