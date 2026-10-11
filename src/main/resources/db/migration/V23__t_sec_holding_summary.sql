-- ============================================================
-- V23 — Precomputed SEC 13F Holding Summary
-- Range-partitioned by report_period (yearly).
-- Populated incrementally by SecHoldingSummaryService in Java
-- after every bulk 13F load.
-- ============================================================
CREATE TABLE t_sec_holding_summary (
    cusip                      VARCHAR(9)    NOT NULL,
    report_period              DATE          NOT NULL,

    -- Portfolio Holdings State (from t_sec_holding)
    total_inst_value_usd       BIGINT        NOT NULL DEFAULT 0,
    total_inst_shares          BIGINT        NOT NULL DEFAULT 0,
    total_holders_count        INTEGER       NOT NULL DEFAULT 0,

    -- Ownership Concentration (Top 5 Holders)
    top_holders_names          TEXT[],
    top5_holders_value_usd     BIGINT        NOT NULL DEFAULT 0,
    top5_holders_pct           NUMERIC(5,2),

    -- Institutional Option Exposure & Sentiment (from t_sec_holding)
    put_value_usd              BIGINT        NOT NULL DEFAULT 0,
    call_value_usd             BIGINT        NOT NULL DEFAULT 0,
    put_call_ratio             NUMERIC(12,2),

    -- Quarter-over-Quarter Net Flows (from t_sec_holding_delta)
    net_value_change_usd       BIGINT        NOT NULL DEFAULT 0,
    net_shares_change          BIGINT        NOT NULL DEFAULT 0,

    -- Fund Activity Breadth
    funds_buying_count         INTEGER       NOT NULL DEFAULT 0,
    funds_selling_count        INTEGER       NOT NULL DEFAULT 0,
    funds_new_count            INTEGER       NOT NULL DEFAULT 0,
    funds_closed_count         INTEGER       NOT NULL DEFAULT 0,
    funds_unchanged_count      INTEGER       NOT NULL DEFAULT 0,

    -- Tier 1 Superinvestors / Elite Funds (t_sec_filer.tier = 1)
    tier1_value_usd            BIGINT        NOT NULL DEFAULT 0,
    tier1_shares               BIGINT        NOT NULL DEFAULT 0,
    tier1_holders_count        INTEGER       NOT NULL DEFAULT 0,
    tier1_net_value_change_usd BIGINT        NOT NULL DEFAULT 0,
    tier1_net_shares_change    BIGINT        NOT NULL DEFAULT 0,
    tier1_buyers_count         INTEGER       NOT NULL DEFAULT 0,
    tier1_sellers_count        INTEGER       NOT NULL DEFAULT 0,
    tier1_new_count            INTEGER       NOT NULL DEFAULT 0,
    tier1_closed_count         INTEGER       NOT NULL DEFAULT 0,
    tier1_buyer_names          TEXT[],
    tier1_seller_names         TEXT[],

    -- Timestamps
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (cusip, report_period)
) PARTITION BY RANGE (report_period);

CREATE INDEX ix_sec_holding_summary_period ON t_sec_holding_summary USING brin (report_period);

