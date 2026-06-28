-- Flyway repeatable migration script for screener views
-- MUST RUN LAST. Depends on core views and alternative data.

DROP VIEW IF EXISTS v_screener_master CASCADE;
DROP VIEW IF EXISTS v_screener_news_sentiment CASCADE;
DROP VIEW IF EXISTS v_screener_undervalued CASCADE;
DROP VIEW IF EXISTS v_screener_quality_compounders CASCADE;
DROP VIEW IF EXISTS v_screener_graham_deep_value CASCADE;
DROP VIEW IF EXISTS v_screener_financial_trend CASCADE;
DROP VIEW IF EXISTS v_screener_insider_activity CASCADE;

-- =================================================================
-- VIEW: v_screener_master
-- Description: Master screener with all valid latest records and rankings
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating thin screener views...'; END $$;
CREATE VIEW v_screener_master AS
SELECT s.*,
       n.news_sentiment_30d as news_sentiment_score,
       n.news_sentiment_label_30d as news_sentiment_label,
       n.news_count_30d,

       RANK() OVER (ORDER BY s.quality_score DESC NULLS LAST) AS quality_rank,
       RANK() OVER (ORDER BY s.earnings_yield DESC NULLS LAST) AS value_rank,
       RANK() OVER (ORDER BY s.financial_trend_score DESC NULLS LAST) AS fscore_rank
FROM mv_share_price_signals_recent s
LEFT JOIN v_ticker_news_sentiment n ON n.ticker = s.ticker
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND s.market_capitalization IS NOT NULL;

-- =================================================================
-- VIEW: v_screener_news_sentiment
-- Description: Detailed news sentiment screener view
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_screener_news_sentiment'; END $$;
CREATE VIEW v_screener_news_sentiment AS
SELECT s.*,
       n.news_count_7d,
       n.news_sentiment_7d,
       n.avg_relevance_7d,
       n.news_sentiment_label_7d,
       
       n.news_count_14d,
       n.news_sentiment_14d,
       n.avg_relevance_14d,
       n.news_sentiment_label_14d,
       
       n.news_count_30d,
       n.news_sentiment_30d,
       n.avg_relevance_30d,
       n.news_sentiment_label_30d
FROM mv_share_price_signals_recent s
LEFT JOIN v_ticker_news_sentiment n ON n.ticker = s.ticker
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND s.market_capitalization IS NOT NULL;

-- =================================================================
-- VIEW: v_screener_undervalued
-- Description: Value / GARP strategy
-- =================================================================
CREATE VIEW v_screener_undervalued AS
SELECT s.*
FROM mv_share_price_signals_recent s
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND s.quality_score >= 50
  AND (s.earnings_yield > 0.05 OR s.fcf_yield > 0.05 OR (s.pe_ratio > 0 AND s.pe_ratio <= 15.0))
  AND s.financial_trend_score >= 5;

-- =================================================================
-- VIEW: v_screener_quality_compounders
-- Description: Consistent Growth strategy
-- =================================================================
CREATE VIEW v_screener_quality_compounders AS
SELECT s.*
FROM mv_share_price_signals_recent s
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND s.quality_score >= 70
  AND s.revenue_growth_streak >= 3
  AND s.positive_fcf_streak >= 3
  AND s.financial_trend_score >= 6;

-- =================================================================
-- VIEW: v_screener_graham_deep_value
-- Description: Asset / Defensive strategy
-- =================================================================
CREATE VIEW v_screener_graham_deep_value AS
SELECT s.*
FROM mv_share_price_signals_recent s
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND (s.is_graham_net_net = 1 OR s.is_graham_defensive = 1 OR s.price_to_ncav < 1.0)
  AND s.current_ratio > 1.5;

-- =================================================================
-- VIEW: v_screener_financial_trend
-- Description: Fundamental Health strategy
-- =================================================================
CREATE VIEW v_screener_financial_trend AS
SELECT s.*
FROM mv_share_price_signals_recent s
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND s.financial_trend_score >= 7;

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
    n.news_sentiment_30d       AS news_sentiment_score,
    n.news_sentiment_label_30d AS news_sentiment_label,
    n.news_count_30d,
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
FROM mv_share_price_signals_recent s
LEFT JOIN v_ticker_news_sentiment       n   ON n.ticker   = s.ticker
LEFT JOIN v_insider_transaction_summary its ON its.ticker = s.ticker
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND s.market_capitalization IS NOT NULL;

