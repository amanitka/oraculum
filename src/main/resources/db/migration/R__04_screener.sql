-- Flyway repeatable migration script for screener views
-- MUST RUN LAST. Depends on core views and alternative data.
DROP MATERIALIZED VIEW IF EXISTS mv_company_overview CASCADE;
DROP VIEW IF EXISTS v_screener_news_sentiment CASCADE;
DROP VIEW IF EXISTS v_screener_undervalued CASCADE;
DROP VIEW IF EXISTS v_screener_quality_compounders CASCADE;
DROP VIEW IF EXISTS v_screener_graham_deep_value CASCADE;
DROP VIEW IF EXISTS v_screener_financial_trend CASCADE;
DROP VIEW IF EXISTS v_screener_insider_activity CASCADE;
DROP VIEW IF EXISTS v_ticker_document_pending CASCADE;

-- =================================================================
-- VIEW: mv_company_overview
-- Description: Master company overview with all valid latest records and rankings
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating materialized view: mv_company_overview...'; END $$;
CREATE MATERIALIZED VIEW mv_company_overview AS
WITH recent_with_lags AS (
    SELECT *,
           LAG(share_price, 1) OVER (PARTITION BY company_id ORDER BY trade_date) as close_1d_ago,
           LAG(share_price, 5) OVER (PARTITION BY company_id ORDER BY trade_date) as close_1w_ago,
           LAG(share_price, 21) OVER (PARTITION BY company_id ORDER BY trade_date) as close_1m_ago
    FROM mv_share_price_signals_recent
),
latest_signals AS (
    SELECT *,
           ROUND(((share_price - close_1d_ago) / NULLIF(close_1d_ago, 0) * 100)::numeric, 2) AS price_change_1d,
           ROUND(((share_price - close_1w_ago) / NULLIF(close_1w_ago, 0) * 100)::numeric, 2) AS price_change_1w,
           ROUND(((share_price - close_1m_ago) / NULLIF(close_1m_ago, 0) * 100)::numeric, 2) AS price_change_1m
    FROM recent_with_lags
    WHERE trade_date = (SELECT MAX(trade_date) FROM recent_with_lags r2 WHERE r2.company_id = recent_with_lags.company_id)
)
SELECT s.*,
       n.news_count_7d, n.news_sentiment_7d, n.avg_relevance_7d, n.news_sentiment_label_7d,
       n.news_count_14d, n.news_sentiment_14d, n.avg_relevance_14d, n.news_sentiment_label_14d,
       n.news_sentiment_30d as news_sentiment_score,
       n.news_sentiment_label_30d as news_sentiment_label,
       n.news_count_30d, n.news_sentiment_30d, n.avg_relevance_30d, n.news_sentiment_label_30d,
       RANK() OVER (ORDER BY s.quality_score DESC NULLS LAST) AS quality_rank,
       RANK() OVER (ORDER BY s.earnings_yield DESC NULLS LAST) AS value_rank,
       RANK() OVER (ORDER BY s.financial_trend_score DESC NULLS LAST) AS fscore_rank
FROM latest_signals s
LEFT JOIN v_ticker_news_sentiment n ON n.ticker = s.ticker
WHERE s.market_capitalization IS NOT NULL;

CREATE UNIQUE INDEX idx_mv_company_overview_company_id 
ON mv_company_overview (company_id);

ANALYZE mv_company_overview;

-- =================================================================
-- VIEW: v_screener_news_sentiment
-- Description: Detailed news sentiment screener view
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_screener_news_sentiment'; END $$;
CREATE VIEW v_screener_news_sentiment AS
SELECT *
FROM mv_company_overview;

-- =================================================================
-- VIEW: v_screener_undervalued
-- Description: Value / GARP strategy
-- =================================================================
CREATE VIEW v_screener_undervalued AS
SELECT *
FROM mv_company_overview
WHERE quality_score >= 50
  AND (earnings_yield > 0.05 OR fcf_yield > 0.05 OR (pe_ratio > 0 AND pe_ratio <= 15.0))
  AND financial_trend_score >= 5;

-- =================================================================
-- VIEW: v_screener_quality_compounders
-- Description: Consistent Growth strategy
-- =================================================================
CREATE VIEW v_screener_quality_compounders AS
SELECT *
FROM mv_company_overview
WHERE quality_score >= 70
  AND revenue_growth_streak >= 3
  AND positive_fcf_streak >= 3
  AND financial_trend_score >= 6;

-- =================================================================
-- VIEW: v_screener_graham_deep_value
-- Description: Asset / Defensive strategy
-- =================================================================
CREATE VIEW v_screener_graham_deep_value AS
SELECT *
FROM mv_company_overview
WHERE (is_graham_net_net = 1 OR is_graham_defensive = 1 OR price_to_ncav < 1.0)
  AND current_ratio > 1.5;

-- =================================================================
-- VIEW: v_screener_financial_trend
-- Description: Fundamental Health strategy
-- =================================================================
CREATE VIEW v_screener_financial_trend AS
SELECT *
FROM mv_company_overview
WHERE financial_trend_score >= 7;

-- =================================================================
-- VIEW: v_screener_insider_activity
-- Description: Screener combining share price signals (mv_share_price_signals_recent),
--              news sentiment (v_ticker_news_sentiment) and insider activity
--              (v_insider_transaction_summary). Enables finding companies where
--              management is actively buying while news sentiment is negative.
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_screener_insider_activity'; END $$;
CREATE VIEW v_screener_insider_activity AS
SELECT
    s.*,
    its.buys_value_3m,
    its.sells_value_3m,
    its.csuite_buys_count_3m,
    its.csuite_buys_value_3m,
    its.buys_value_6m,
    its.sells_value_6m,
    its.csuite_buys_count_6m,
    its.csuite_buys_value_6m,
    its.buys_value_12m,
    its.sells_value_12m,
    its.csuite_buys_count_12m,
    its.csuite_buys_value_12m,
    its.has_cluster_buy
FROM mv_company_overview s
LEFT JOIN v_insider_transaction_summary its ON its.ticker = s.ticker;

-- =================================================================
-- VIEW: v_ticker_document_pending
-- Description: Prioritized queue of pending raw SEC documents to process.
--              Orders documents for largest companies first, then by report period DESC.
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_ticker_document_pending'; END $$;

CREATE VIEW v_ticker_document_pending AS
SELECT
    r.id,
    r.ticker,
    r.market,
    r.document_type,
    r.document_subtype,
    r.report_period,
    r.filing_date,
    r.source_url,
    r.accession_number,
    r.content,
    r.status,
    c.company_name,
    COALESCE(s.market_capitalization, 0) AS market_capitalization,
    COALESCE(s.company_size, 'MICRO')   AS company_size,
    ROW_NUMBER() OVER (
        PARTITION BY r.ticker, r.market, CASE WHEN r.document_type IN ('SEC_10K', 'SEC_10Q') THEN 'SEC_10X' ELSE r.document_type END, r.document_subtype
        ORDER BY r.report_period DESC, r.filing_date DESC
    ) AS document_priority
FROM t_ticker_document_raw r
JOIN t_company c ON c.ticker = r.ticker
                AND c.market = r.market
LEFT JOIN mv_company_overview s ON s.company_id = c.id
ORDER BY COALESCE(s.market_capitalization, 0) DESC,
         r.ticker,
         r.report_period DESC;


