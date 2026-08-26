-- ============================================================
-- V21 — SEC 13F Institutional Holdings
-- ============================================================
-- Tables:
--   t_sec_filer        — one row per institutional manager (CIK)
--   t_sec_holding      — partitioned by period_of_report (quarterly)
--   t_sec_holding_delta — partitioned by period_of_report (quarterly)
-- ============================================================

-- ── Enum: filer tier ─────────────────────────────────────────
CREATE TYPE sec_filer_tier AS ENUM ('TIER_1', 'TIER_2');

-- ── t_sec_filer ───────────────────────────────────────────────
CREATE TABLE t_sec_filer (
    cik                        VARCHAR(10)     PRIMARY KEY,
    manager_name               VARCHAR(255)    NOT NULL,
    is_active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    tier                       sec_filer_tier  NOT NULL DEFAULT 'TIER_2',
    last_filing_date           DATE,
    last_processed_accession   VARCHAR(25),
    created_at                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sec_filer_tier ON t_sec_filer (tier);

-- ── t_sec_holding (range-partitioned on period_of_report) ─────
CREATE TABLE t_sec_holding (
    id                    BIGSERIAL,
    cik                   VARCHAR(10)   NOT NULL,
    accession_number      VARCHAR(25)   NOT NULL,
    period_of_report      DATE          NOT NULL,
    filing_date           DATE          NOT NULL,
    manager_name          VARCHAR(255)  NOT NULL,
    issuer_name           VARCHAR(255)  NOT NULL,
    class_title           VARCHAR(100),
    cusip                 VARCHAR(9)    NOT NULL,
    value_usd             BIGINT        NOT NULL DEFAULT 0,
    shares_or_prn_amount  BIGINT        NOT NULL DEFAULT 0,
    shares_or_prn_type    VARCHAR(4),
    option_type           VARCHAR(4),
    investment_discretion VARCHAR(4),
    voting_auth_sole      BIGINT        NOT NULL DEFAULT 0,
    voting_auth_shared    BIGINT        NOT NULL DEFAULT 0,
    voting_auth_none      BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sec_holding UNIQUE (accession_number, cusip, COALESCE(option_type, ''), period_of_report)
) PARTITION BY RANGE (period_of_report);

CREATE INDEX idx_sec_holding_cik     ON t_sec_holding (cik, period_of_report);
CREATE INDEX idx_sec_holding_cusip   ON t_sec_holding (cusip, period_of_report);

-- ── t_sec_holding_delta (range-partitioned on period_of_report) ──
CREATE TABLE t_sec_holding_delta (
    id                    BIGSERIAL,
    cik                   VARCHAR(10)   NOT NULL,
    period_of_report      DATE          NOT NULL,
    cusip                 VARCHAR(9)    NOT NULL,
    issuer_name           VARCHAR(255)  NOT NULL,
    value_usd_current     BIGINT        NOT NULL DEFAULT 0,
    value_usd_previous    BIGINT,
    value_usd_change      BIGINT,
    shares_current        BIGINT        NOT NULL DEFAULT 0,
    shares_previous       BIGINT,
    shares_change         BIGINT,
    is_new_position       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_closed_position    BOOLEAN       NOT NULL DEFAULT FALSE,
    computed_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sec_holding_delta UNIQUE (cik, cusip, period_of_report)
) PARTITION BY RANGE (period_of_report);

CREATE INDEX idx_sec_holding_delta_cik   ON t_sec_holding_delta (cik, period_of_report);
CREATE INDEX idx_sec_holding_delta_cusip ON t_sec_holding_delta (cusip, period_of_report);



