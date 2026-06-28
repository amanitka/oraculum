-- Flyway repeatable migration script for sentiment and insider views

DROP VIEW IF EXISTS v_ticker_news_sentiment CASCADE;
DROP VIEW IF EXISTS v_insider_transaction_summary CASCADE;

-- =================================================================
-- VIEW: v_ticker_news_sentiment
-- Description: 7, 14, and 30-day time-decayed relevance-weighted sentiment
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_ticker_news_sentiment'; END $$;
CREATE VIEW v_ticker_news_sentiment AS
WITH sentiment_calc AS (
    SELECT 
        ticker,
        
        -- 7 Days
        COUNT(CASE WHEN time_published >= NOW() - INTERVAL '7 days' THEN 1 END) as news_count_7d,
        SUM(CASE WHEN time_published >= NOW() - INTERVAL '7 days' THEN ticker_sentiment_score * relevance_score * EXP(-0.231 * (GREATEST(0, EXTRACT(EPOCH FROM (NOW() - time_published)) / 86400.0))) END)
        / NULLIF(SUM(CASE WHEN time_published >= NOW() - INTERVAL '7 days' THEN relevance_score * EXP(-0.231 * (GREATEST(0, EXTRACT(EPOCH FROM (NOW() - time_published)) / 86400.0))) END), 0) as news_sentiment_7d,
        AVG(CASE WHEN time_published >= NOW() - INTERVAL '7 days' THEN relevance_score END) as avg_relevance_7d,

        -- 14 Days
        COUNT(CASE WHEN time_published >= NOW() - INTERVAL '14 days' THEN 1 END) as news_count_14d,
        SUM(CASE WHEN time_published >= NOW() - INTERVAL '14 days' THEN ticker_sentiment_score * relevance_score * EXP(-0.231 * (GREATEST(0, EXTRACT(EPOCH FROM (NOW() - time_published)) / 86400.0))) END)
        / NULLIF(SUM(CASE WHEN time_published >= NOW() - INTERVAL '14 days' THEN relevance_score * EXP(-0.231 * (GREATEST(0, EXTRACT(EPOCH FROM (NOW() - time_published)) / 86400.0))) END), 0) as news_sentiment_14d,
        AVG(CASE WHEN time_published >= NOW() - INTERVAL '14 days' THEN relevance_score END) as avg_relevance_14d,

        -- 30 Days
        COUNT(CASE WHEN time_published >= NOW() - INTERVAL '30 days' THEN 1 END) as news_count_30d,
        SUM(CASE WHEN time_published >= NOW() - INTERVAL '30 days' THEN ticker_sentiment_score * relevance_score * EXP(-0.231 * (GREATEST(0, EXTRACT(EPOCH FROM (NOW() - time_published)) / 86400.0))) END)
        / NULLIF(SUM(CASE WHEN time_published >= NOW() - INTERVAL '30 days' THEN relevance_score * EXP(-0.231 * (GREATEST(0, EXTRACT(EPOCH FROM (NOW() - time_published)) / 86400.0))) END), 0) as news_sentiment_30d,
        AVG(CASE WHEN time_published >= NOW() - INTERVAL '30 days' THEN relevance_score END) as avg_relevance_30d

    FROM public.t_news_ticker
    WHERE time_published >= NOW() - INTERVAL '30 days'
    GROUP BY ticker
)
SELECT 
    ticker,
    
    -- 7 Days projected
    news_count_7d,
    ROUND(news_sentiment_7d::numeric, 4) as news_sentiment_7d,
    ROUND(avg_relevance_7d::numeric, 4) as avg_relevance_7d,
    CASE
        WHEN news_sentiment_7d IS NULL THEN 'NO_DATA'
        WHEN news_sentiment_7d <= -0.35 THEN 'BEARISH'
        WHEN news_sentiment_7d <= -0.15 THEN 'SOMEWHAT_BEARISH'
        WHEN news_sentiment_7d < 0.15 THEN 'NEUTRAL'
        WHEN news_sentiment_7d < 0.35 THEN 'SOMEWHAT_BULLISH'
        ELSE 'BULLISH'
    END as news_sentiment_label_7d,

    -- 14 Days projected
    news_count_14d,
    ROUND(news_sentiment_14d::numeric, 4) as news_sentiment_14d,
    ROUND(avg_relevance_14d::numeric, 4) as avg_relevance_14d,
    CASE
        WHEN news_sentiment_14d IS NULL THEN 'NO_DATA'
        WHEN news_sentiment_14d <= -0.35 THEN 'BEARISH'
        WHEN news_sentiment_14d <= -0.15 THEN 'SOMEWHAT_BEARISH'
        WHEN news_sentiment_14d < 0.15 THEN 'NEUTRAL'
        WHEN news_sentiment_14d < 0.35 THEN 'SOMEWHAT_BULLISH'
        ELSE 'BULLISH'
    END as news_sentiment_label_14d,

    -- 30 Days projected
    news_count_30d,
    ROUND(news_sentiment_30d::numeric, 4) as news_sentiment_30d,
    ROUND(avg_relevance_30d::numeric, 4) as avg_relevance_30d,
    CASE
        WHEN news_sentiment_30d IS NULL THEN 'NO_DATA'
        WHEN news_sentiment_30d <= -0.35 THEN 'BEARISH'
        WHEN news_sentiment_30d <= -0.15 THEN 'SOMEWHAT_BEARISH'
        WHEN news_sentiment_30d < 0.15 THEN 'NEUTRAL'
        WHEN news_sentiment_30d < 0.35 THEN 'SOMEWHAT_BULLISH'
        ELSE 'BULLISH'
    END as news_sentiment_label_30d

FROM sentiment_calc;

-- =================================================================
-- VIEW: v_insider_transaction_summary
-- Description: Aggregates insider transactions across 3M, 6M and 12M windows.
--              Provides buy/sell volumes, C-Suite activity and Cluster Buy detection
--              at each horizon, mirroring the multi-window approach of v_ticker_news_sentiment.
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_insider_transaction_summary'; END $$;
CREATE VIEW v_insider_transaction_summary AS
WITH base AS (
    SELECT
        ticker,
        trade_type,
        value,
        title,
        insider_name,
        filing_date
    FROM t_insider_transaction_ticker
    WHERE filing_date >= NOW() - INTERVAL '1 year'
),
csuite_filter AS (
    SELECT * FROM base
    WHERE title ILIKE '%CEO%' OR title ILIKE '%CFO%' OR title ILIKE '%COO%' OR title ILIKE '%President%'
),
aggregates AS (
    SELECT
        ticker,

        -- 3 Month window
        SUM(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '3 months' THEN value ELSE 0 END) AS buys_value_3m,
        SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '3 months' THEN value ELSE 0 END) AS sells_value_3m,

        -- 6 Month window
        SUM(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '6 months' THEN value ELSE 0 END) AS buys_value_6m,
        SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '6 months' THEN value ELSE 0 END) AS sells_value_6m,

        -- 12 Month window (LTM)
        SUM(CASE WHEN trade_type LIKE 'P - Purchase%' THEN value ELSE 0 END) AS buys_value_12m,
        SUM(CASE WHEN trade_type LIKE 'S - Sale%'     THEN value ELSE 0 END) AS sells_value_12m
    FROM base
    GROUP BY ticker
),
csuite_aggregates AS (
    SELECT
        ticker,

        -- 3 Month C-Suite
        COUNT(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '3 months' THEN 1 END) AS csuite_buys_count_3m,
        SUM(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '3 months' THEN value ELSE 0 END) AS csuite_buys_value_3m,

        -- 6 Month C-Suite
        COUNT(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '6 months' THEN 1 END) AS csuite_buys_count_6m,
        SUM(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '6 months' THEN value ELSE 0 END) AS csuite_buys_value_6m,

        -- 12 Month C-Suite
        COUNT(CASE WHEN trade_type LIKE 'P - Purchase%' THEN 1 END) AS csuite_buys_count_12m,
        SUM(CASE WHEN trade_type LIKE 'P - Purchase%' THEN value ELSE 0 END)  AS csuite_buys_value_12m
    FROM csuite_filter
    GROUP BY ticker
),
cluster_buys_monthly AS (
    SELECT ticker, DATE_TRUNC('month', filing_date) AS m, COUNT(DISTINCT insider_name) AS distinct_buyers
    FROM base
    WHERE trade_type LIKE 'P - Purchase%'
    GROUP BY ticker, DATE_TRUNC('month', filing_date)
),
cluster_buys AS (
    SELECT DISTINCT ticker FROM cluster_buys_monthly WHERE distinct_buyers >= 3
)
SELECT
    a.ticker,

    -- 3 Month
    COALESCE(a.buys_value_3m, 0)            AS buys_value_3m,
    COALESCE(a.sells_value_3m, 0)           AS sells_value_3m,
    COALESCE(cs.csuite_buys_count_3m, 0)    AS csuite_buys_count_3m,
    COALESCE(cs.csuite_buys_value_3m, 0)    AS csuite_buys_value_3m,

    -- 6 Month
    COALESCE(a.buys_value_6m, 0)            AS buys_value_6m,
    COALESCE(a.sells_value_6m, 0)           AS sells_value_6m,
    COALESCE(cs.csuite_buys_count_6m, 0)    AS csuite_buys_count_6m,
    COALESCE(cs.csuite_buys_value_6m, 0)    AS csuite_buys_value_6m,

    -- 12 Month (LTM)
    COALESCE(a.buys_value_12m, 0)           AS buys_value_12m,
    COALESCE(a.sells_value_12m, 0)          AS sells_value_12m,
    COALESCE(cs.csuite_buys_count_12m, 0)   AS csuite_buys_count_12m,
    COALESCE(cs.csuite_buys_value_12m, 0)   AS csuite_buys_value_12m,

    -- Cluster Buy flag (any month within LTM with 3+ distinct insider buyers)
    CASE WHEN cb.ticker IS NOT NULL THEN true ELSE false END AS has_cluster_buy
FROM aggregates a
LEFT JOIN csuite_aggregates cs ON a.ticker = cs.ticker
LEFT JOIN cluster_buys cb      ON a.ticker = cb.ticker;

