-- Flyway repeatable migration script for macroeconomic views

DROP VIEW IF EXISTS v_macro_summary CASCADE;

-- =================================================================
-- View: v_macro_summary
-- Summarizes raw macroeconomic data into 1-year trends, YoY changes, and moving averages.
-- Used to provide high-density analytical context to LLM agents.
-- =================================================================
CREATE OR REPLACE VIEW public.v_macro_summary AS
WITH latest_dates AS (
    SELECT indicator_code, MAX(observation_date) as max_date
    FROM public.t_macro_observation
    GROUP BY indicator_code
),
latest_values AS (
    SELECT o.indicator_code, o.observation_date as latest_date, o.value as latest_value
    FROM public.t_macro_observation o
    JOIN latest_dates l ON o.indicator_code = l.indicator_code AND o.observation_date = l.max_date
),
stats_1y AS (
    SELECT 
        o.indicator_code,
        MIN(o.value) as min_1y,
        MAX(o.value) as max_1y,
        AVG(o.value) as avg_1y
    FROM public.t_macro_observation o
    JOIN latest_dates l ON o.indicator_code = l.indicator_code
    WHERE o.observation_date >= l.max_date - INTERVAL '1 year'
    GROUP BY o.indicator_code
),
values_1y_ago AS (
    SELECT DISTINCT ON (o.indicator_code) 
        o.indicator_code, o.value as value_1y_ago
    FROM public.t_macro_observation o
    JOIN latest_dates l ON o.indicator_code = l.indicator_code
    WHERE o.observation_date <= l.max_date - INTERVAL '1 year'
    ORDER BY o.indicator_code, o.observation_date DESC
)
SELECT 
    lv.indicator_code,
    lv.latest_date,
    lv.latest_value,
    v1y.value_1y_ago,
    CASE WHEN v1y.value_1y_ago != 0 THEN ROUND((((lv.latest_value - v1y.value_1y_ago) / ABS(v1y.value_1y_ago)) * 100), 3) ELSE NULL END as yoy_change_pct,
    s1.min_1y,
    s1.max_1y,
    ROUND(s1.avg_1y, 3) as avg_1y,
    ROUND((lv.latest_value - s1.avg_1y), 3) as diff_from_1y_avg
FROM latest_values lv
LEFT JOIN stats_1y s1 ON lv.indicator_code = s1.indicator_code
LEFT JOIN values_1y_ago v1y ON lv.indicator_code = v1y.indicator_code;
