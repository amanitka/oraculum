-- Flyway repeatable migration script for sentiment and insider views
DROP VIEW IF EXISTS v_ticker_news_sentiment CASCADE;
DROP VIEW IF EXISTS v_insider_transaction_summary CASCADE;
DROP VIEW IF EXISTS v_insider_transaction_by_person CASCADE;

-- =================================================================
-- VIEW: v_insider_transaction_summary
-- Description: Aggregates insider transactions across 3M, 6M and 12M windows.
--              Provides buy/sell volumes, C-Suite activity and Cluster Buy detection
--              at each horizon across multiple time windows (3M, 6M, 12M).
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

-- =================================================================
-- VIEW: v_insider_transaction_by_person
-- Description: Aggregates insider transactions per individual person
--              across 1M, 3M, 6M, and 12M horizons with initial and
--              current stake change percentages.
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_insider_transaction_by_person'; END $$;
CREATE VIEW v_insider_transaction_by_person AS
WITH tx_12m AS (
    SELECT ticker, insider_name, title, trade_date, filing_date, trade_type, qty, value, owned
    FROM t_insider_transaction_ticker
    WHERE filing_date >= NOW() - INTERVAL '1 year'
),
ranked AS (
    SELECT *,
           ROW_NUMBER() OVER (PARTITION BY ticker, insider_name ORDER BY trade_date ASC, filing_date ASC) as rn_first_12m,
           ROW_NUMBER() OVER (PARTITION BY ticker, insider_name ORDER BY trade_date DESC, filing_date DESC) as rn_last
    FROM tx_12m
)
SELECT 
    ticker || '_' || insider_name as id,
    ticker,
    insider_name,
    MAX(title) as title,
    
    -- 1 Month window
    COUNT(*) FILTER (WHERE filing_date >= NOW() - INTERVAL '1 month') as tx_count_1m,
    SUM(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '1 month' THEN qty ELSE 0 END) as shares_bought_1m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '1 month' THEN ABS(qty) ELSE 0 END) as shares_sold_1m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '1 month' THEN ABS(value) ELSE 0 END) as value_sold_1m,

    -- 3 Month window
    COUNT(*) FILTER (WHERE filing_date >= NOW() - INTERVAL '3 months') as tx_count_3m,
    SUM(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '3 months' THEN qty ELSE 0 END) as shares_bought_3m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '3 months' THEN ABS(qty) ELSE 0 END) as shares_sold_3m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '3 months' THEN ABS(value) ELSE 0 END) as value_sold_3m,

    -- 6 Month window
    COUNT(*) FILTER (WHERE filing_date >= NOW() - INTERVAL '6 months') as tx_count_6m,
    SUM(CASE WHEN trade_type LIKE 'P - Purchase%' AND filing_date >= NOW() - INTERVAL '6 months' THEN qty ELSE 0 END) as shares_bought_6m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '6 months' THEN ABS(qty) ELSE 0 END) as shares_sold_6m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     AND filing_date >= NOW() - INTERVAL '6 months' THEN ABS(value) ELSE 0 END) as value_sold_6m,

    -- 12 Month window (LTM)
    COUNT(*) as tx_count_12m,
    SUM(CASE WHEN trade_type LIKE 'P - Purchase%' THEN qty ELSE 0 END) as shares_bought_12m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     THEN ABS(qty) ELSE 0 END) as shares_sold_12m,
    SUM(CASE WHEN trade_type LIKE 'S - Sale%'     THEN ABS(value) ELSE 0 END) as value_sold_12m,
    
    -- Stock holdings & stake change
    MAX(CASE WHEN rn_first_12m = 1 THEN owned END) as initial_owned_12m,
    MAX(CASE WHEN rn_last = 1 THEN owned END)      as current_owned,
    ROUND(((MAX(CASE WHEN rn_last = 1 THEN owned END) - MAX(CASE WHEN rn_first_12m = 1 THEN owned END)) * 100.0) 
          / NULLIF(MAX(CASE WHEN rn_first_12m = 1 THEN owned END), 0), 2) as stake_change_pct_12m
FROM ranked
GROUP BY ticker, insider_name;


