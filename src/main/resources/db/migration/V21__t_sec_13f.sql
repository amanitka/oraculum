-- ============================================================
-- V21 — SEC 13F filer metadata
-- One row per institutional manager (CIK).
-- ============================================================

CREATE TABLE t_sec_filer (
    cik                        VARCHAR(10)   PRIMARY KEY,
    manager_name               VARCHAR(255)  NOT NULL,
    is_active                  BOOLEAN       NOT NULL,
    tier                       SMALLINT      NOT NULL,
    last_filing_date           DATE,
    last_processed_accession   VARCHAR(25),
    created_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
);

CREATE INDEX ix_sec_filer_tier ON t_sec_filer (tier);
