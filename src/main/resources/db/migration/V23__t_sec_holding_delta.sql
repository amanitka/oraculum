-- ============================================================
-- V23 — SEC 13F holdings delta (quarter-over-quarter changes)
-- Range-partitioned by report_period (quarterly).
-- Populated by SecHoldingDeltaService in Java after every bulk load.
-- ============================================================
CREATE TABLE t_sec_holding_delta (
    id                    BIGSERIAL,
    cik                   VARCHAR(10)   NOT NULL,
    report_period         DATE          NOT NULL,
    cusip                 VARCHAR(9)    NOT NULL,
    issuer_name           VARCHAR(255)  NOT NULL,
    option_type           VARCHAR(4),
    value_usd_current     BIGINT        NOT NULL DEFAULT 0,
    value_usd_previous    BIGINT,
    value_usd_change      BIGINT,
    shares_current        BIGINT        NOT NULL DEFAULT 0,
    shares_previous       BIGINT,
    shares_change         BIGINT,
    is_new_position       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_closed_position    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, report_period),
    CONSTRAINT uq_sec_holding_delta UNIQUE NULLS NOT DISTINCT (cik, cusip, option_type, report_period)
) PARTITION BY RANGE (report_period);

CREATE INDEX ix_sec_holding_delta_cik   ON t_sec_holding_delta (cik);
CREATE INDEX ix_sec_holding_delta_cusip ON t_sec_holding_delta (cusip);
