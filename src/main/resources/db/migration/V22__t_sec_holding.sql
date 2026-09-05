-- ============================================================
-- V22 — SEC 13F holdings (INFOTABLE)
-- Range-partitioned by report_period (quarterly).
-- ============================================================

CREATE TABLE t_sec_holding (
    id                    BIGSERIAL,
    cik                   VARCHAR(10)   NOT NULL,
    accession_number      VARCHAR(25)   NOT NULL,
    report_period         DATE          NOT NULL,
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
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sec_holding UNIQUE (accession_number, cusip, COALESCE(option_type, ''), report_period)
) PARTITION BY RANGE (report_period);

CREATE INDEX ix_sec_holding_cik   ON t_sec_holding (cik, report_period);
CREATE INDEX ix_sec_holding_cusip ON t_sec_holding (cusip, report_period);
