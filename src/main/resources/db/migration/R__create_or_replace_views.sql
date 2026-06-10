-- Flyway repeatable migration script for creating/replacing views
-- Description: Core views containing corrected logic and advanced financial metrics, utilizing materialized views for performance

DROP VIEW IF EXISTS v_screener_news_sentiment CASCADE;
DROP VIEW IF EXISTS v_ticker_news_sentiment CASCADE;
DROP VIEW IF EXISTS v_screener_master CASCADE;
DROP VIEW IF EXISTS v_screener_piotroski CASCADE;
DROP VIEW IF EXISTS v_screener_graham_deep_value CASCADE;
DROP VIEW IF EXISTS v_screener_quality_compounders CASCADE;
DROP VIEW IF EXISTS v_screener_undervalued CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_share_price_signals_recent CASCADE;
DROP VIEW IF EXISTS v_share_price_signals CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_company_financial_ratios CASCADE;
DROP VIEW IF EXISTS v_company_financial_ratios CASCADE;

-- =================================================================
-- VIEW: v_company_financial_ratios
-- Description: Core derived metrics for fundamental analysis
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_company_financial_ratios'; END $$;
CREATE VIEW v_company_financial_ratios AS
WITH statement_values AS (SELECT
                             income.id,
                             income.company_id,
                             income.ticker,
                             income.currency,
                             income.template,
                             income.variant,
                             income.fiscal_year,
                             income.fiscal_period,
                             income.report_date,
                             income.publish_date,
                             income.restated_date,
                             NULLIF(income.statement_data ->> 'Revenue'::text, ''::text)::double precision AS revenue,
                             NULLIF(income.statement_data ->> 'Cost of Revenue'::text, ''::text)::double precision AS cost_of_revenue,
                             NULLIF(income.statement_data ->> 'Net Income'::text, ''::text)::double precision AS net_income,
                             NULLIF(income.statement_data ->> 'Operating Income (Loss)'::text, ''::text)::double precision AS operating_income,
                             NULLIF(income.statement_data ->> 'Interest Expense, Net'::text, ''::text)::double precision AS interest_expense_net,
                             NULLIF(income.statement_data ->> 'Income Tax (Expense) Benefit, Net'::text, ''::text)::double precision AS income_tax,
                             NULLIF(cash_flow.statement_data ->> 'Depreciation & Amortization'::text, ''::text)::double precision AS depreciation_amortization,
                             NULLIF(balance.statement_data ->> 'Total Equity'::text, ''::text)::double precision AS total_equity,
                             NULLIF(balance.statement_data ->> 'Total Assets'::text, ''::text)::double precision AS total_assets,
                             NULLIF(balance.statement_data ->> 'Total Current Assets'::text, ''::text)::double precision AS total_current_assets,
                             NULLIF(balance.statement_data ->> 'Total Current Liabilities'::text, ''::text)::double precision AS total_current_liabilities,
                             NULLIF(balance.statement_data ->> 'Total Liabilities'::text, ''::text)::double precision AS total_liabilities,
                             NULLIF(balance.statement_data ->> 'Cash, Cash Equivalents & Short Term Investments'::text, ''::text)::double precision AS cash_equivalents_short_term_investments,
                             NULLIF(balance.statement_data ->> 'Accounts & Notes Receivable'::text, ''::text)::double precision AS accounts_notes_receivable,
                             NULLIF(balance.statement_data ->> 'Inventories'::text, ''::text)::double precision AS inventories,
                             NULLIF(income.statement_data ->> 'Shares (Basic)'::text, ''::text)::double precision AS shares_basic,
                             NULLIF(income.statement_data ->> 'Shares (Diluted)'::text, ''::text)::double precision AS shares_diluted,
                             NULLIF(cash_flow.statement_data ->> 'Net Cash from Operating Activities'::text, ''::text)::double precision AS net_cash_from_operating_activities,
                             NULLIF(cash_flow.statement_data ->> 'Change in Fixed Assets & Intangibles'::text, ''::text)::double precision AS capital_expenditures
                          FROM t_income_statement income
                          LEFT JOIN t_balance_sheet balance ON balance.company_id = income.company_id
                                                            AND balance.currency = income.currency
                                                            AND balance.template = income.template
                                                            AND balance.variant = income.variant
                                                            AND balance.fiscal_year = income.fiscal_year
                                                            AND CASE WHEN UPPER(balance.variant) = 'ANNUAL' THEN 'FY' ELSE balance.fiscal_period END = income.fiscal_period
                          LEFT JOIN t_cash_flow_statement cash_flow ON cash_flow.company_id = income.company_id
                                                                   AND cash_flow.currency = income.currency
                                                                   AND cash_flow.template = income.template
                                                                   AND cash_flow.variant = income.variant
                                                                   AND cash_flow.fiscal_year = income.fiscal_year
                                                                   AND CASE WHEN UPPER(cash_flow.variant) = 'ANNUAL' THEN 'FY' ELSE cash_flow.fiscal_period END = income.fiscal_period
                          ),
     computed_ratios AS (SELECT
                             id,
                             company_id,
                             ticker,
                             currency,
                             template,
                             variant,
                             fiscal_year,
                             fiscal_period,
                             report_date,
                             publish_date,
                             restated_date,
                             total_equity,
                             total_liabilities,
                             total_assets,
                             cash_equivalents_short_term_investments,
                             net_cash_from_operating_activities,
                             COALESCE(operating_income, 0::double precision) + ABS(COALESCE(depreciation_amortization, 0::double precision)) AS ebitda,
                             COALESCE(net_cash_from_operating_activities, 0::double precision) - ABS(COALESCE(capital_expenditures, 0::double precision)) AS free_cash_flow,
                             COALESCE(total_current_assets, 0::double precision) - ABS(COALESCE(total_liabilities, 0::double precision)) AS ncav,
                             COALESCE(cash_equivalents_short_term_investments, 0::double precision)
                                 + (COALESCE(accounts_notes_receivable, 0::double precision) * 0.75::double precision)
                                 + (COALESCE(inventories, 0::double precision) * 0.5::double precision)
                                 - ABS(COALESCE(total_liabilities, 0::double precision)) AS net_net_working_capital,
                             operating_income / NULLIF(COALESCE(total_assets, total_equity + ABS(COALESCE(total_liabilities, 0::double precision))), 0::double precision) AS return_on_capital_employed,
                             net_income / NULLIF(total_equity, 0::double precision) AS return_on_equity,
                             net_income / NULLIF(COALESCE(total_assets, total_equity + ABS(COALESCE(total_liabilities, 0::double precision))), 0::double precision) AS return_on_assets,
                             net_income / NULLIF(revenue, 0::double precision) AS net_margin,
                             (revenue - ABS(COALESCE(cost_of_revenue, 0::double precision))) / NULLIF(revenue, 0::double precision) AS gross_margin,
                             operating_income / NULLIF(revenue, 0::double precision) AS operating_margin,
                             (COALESCE(net_cash_from_operating_activities, 0::double precision) - ABS(COALESCE(capital_expenditures, 0::double precision))) / NULLIF(revenue, 0::double precision) AS fcf_margin,
                             total_current_assets / NULLIF(ABS(COALESCE(total_current_liabilities, 0::double precision)), 0::double precision) AS current_ratio,
                             (COALESCE(cash_equivalents_short_term_investments, 0::double precision) + COALESCE(accounts_notes_receivable, 0::double precision)) / NULLIF(ABS(COALESCE(total_current_liabilities, 0::double precision)), 0::double precision) AS quick_ratio,
                             ABS(COALESCE(total_liabilities, 0::double precision)) / NULLIF(total_equity, 0::double precision) AS debt_to_equity,
                             operating_income / NULLIF(ABS(COALESCE(interest_expense_net, 0::double precision)), 0::double precision) AS interest_coverage_ratio,
                             ABS(COALESCE(cost_of_revenue, 0::double precision)) / NULLIF(inventories, 0::double precision) AS inventory_turnover,
                             revenue / NULLIF(COALESCE(total_assets, total_equity + ABS(COALESCE(total_liabilities, 0::double precision))), 0::double precision) AS asset_turnover,
                             COALESCE(shares_diluted, shares_basic) AS shares_stabilized,
                             net_income / NULLIF(COALESCE(shares_diluted, shares_basic), 0::double precision) AS earnings_per_share,
                             (COALESCE(net_cash_from_operating_activities, 0::double precision) - ABS(COALESCE(capital_expenditures, 0::double precision)))
                                 / NULLIF(COALESCE(shares_diluted, shares_basic), 0::double precision) AS fcf_per_share,
                             revenue,
                             net_income
                         FROM statement_values
                         ),
     lagged_values AS (SELECT
                             *,
                             LAG(revenue) OVER w AS prev_revenue,
                             LAG(net_income) OVER w AS prev_net_income,
                             LAG(ebitda) OVER w AS prev_ebitda,
                             LAG(free_cash_flow) OVER w AS prev_fcf,
                             LAG(earnings_per_share) OVER w AS prev_eps,
                             LAG(return_on_assets) OVER w AS prev_roa,
                             LAG(debt_to_equity) OVER w AS prev_debt_to_equity,
                             LAG(current_ratio) OVER w AS prev_current_ratio,
                             LAG(shares_stabilized) OVER w AS prev_shares_stabilized,
                             LAG(gross_margin) OVER w AS prev_gross_margin,
                             LAG(asset_turnover) OVER w AS prev_asset_turnover,
                             LAG(operating_margin) OVER w AS prev_operating_margin,
                             LAG(net_margin) OVER w AS prev_net_margin
                       FROM computed_ratios
                       WINDOW
                           w AS (PARTITION BY company_id, UPPER(variant) ORDER BY fiscal_year, fiscal_period)
                       ),
     enriched_windows AS (SELECT
                             *,
                             SUM(CASE WHEN free_cash_flow > 0 THEN 1 ELSE 0 END) OVER w4 AS positive_fcf_streak,
                             SUM(CASE WHEN net_income > 0 THEN 1 ELSE 0 END) OVER w4 AS positive_earnings_streak,
                             SUM(CASE WHEN revenue > prev_revenue THEN 1 ELSE 0 END) OVER w4 AS revenue_growth_streak
                          FROM lagged_values
                          WINDOW
                              w4 AS (PARTITION BY company_id, UPPER(variant) ORDER BY fiscal_year, fiscal_period ROWS BETWEEN 3 PRECEDING AND CURRENT ROW)
                          )
SELECT
    id,
    company_id,
    ticker,
    currency,
    template,
    variant,
    fiscal_year,
    fiscal_period,
    report_date,
    publish_date,
    restated_date,
    total_equity,
    total_liabilities,
    total_assets,
    cash_equivalents_short_term_investments,
    net_cash_from_operating_activities,
    ebitda,
    free_cash_flow,
    ncav,
    net_net_working_capital,
    return_on_capital_employed,
    return_on_equity,
    return_on_assets,
    net_margin,
    gross_margin,
    operating_margin,
    fcf_margin,
    current_ratio,
    quick_ratio,
    debt_to_equity,
    interest_coverage_ratio,
    inventory_turnover,
    asset_turnover,
    shares_stabilized,
    earnings_per_share,
    fcf_per_share,
    revenue,
    net_income,
    
    (revenue - prev_revenue) / NULLIF(ABS(prev_revenue), 0) AS revenue_yoy_growth,
    (net_income - prev_net_income) / NULLIF(ABS(prev_net_income), 0) AS net_income_yoy_growth,
    (ebitda - prev_ebitda) / NULLIF(ABS(prev_ebitda), 0) AS ebitda_yoy_growth,
    (free_cash_flow - prev_fcf) / NULLIF(ABS(prev_fcf), 0) AS fcf_yoy_growth,
    (earnings_per_share - prev_eps) / NULLIF(ABS(prev_eps), 0) AS eps_yoy_growth,
    
    (CASE WHEN return_on_assets > 0 THEN 1 ELSE 0 END) +
    (CASE WHEN net_cash_from_operating_activities > 0 THEN 1 ELSE 0 END) +
    (CASE WHEN return_on_assets > prev_roa THEN 1 ELSE 0 END) +
    (CASE WHEN net_cash_from_operating_activities > net_income THEN 1 ELSE 0 END) +
    (CASE WHEN debt_to_equity < prev_debt_to_equity THEN 1 ELSE 0 END) +
    (CASE WHEN current_ratio > prev_current_ratio THEN 1 ELSE 0 END) +
    (CASE WHEN shares_stabilized <= prev_shares_stabilized THEN 1 ELSE 0 END) +
    (CASE WHEN gross_margin > prev_gross_margin THEN 1 ELSE 0 END) +
    (CASE WHEN asset_turnover > prev_asset_turnover THEN 1 ELSE 0 END) AS piotroski_f_score,
    
    net_cash_from_operating_activities / NULLIF(net_income, 0) AS earnings_quality_ratio,
    (CASE WHEN net_cash_from_operating_activities > net_income THEN 1 ELSE 0 END) AS is_cash_earnings,
    (CASE WHEN total_equity < 0 THEN 1 ELSE 0 END) AS is_negative_equity,
    (CASE WHEN gross_margin > prev_gross_margin 
          AND operating_margin > prev_operating_margin 
          AND net_margin > prev_net_margin THEN 1 ELSE 0 END) AS margin_expansion_signal,
    revenue_growth_streak,
    positive_fcf_streak,
    positive_earnings_streak
FROM enriched_windows;

-- MATERIALIZE Fundamental Ratios
DO $$ BEGIN RAISE NOTICE 'Materializing view: mv_company_financial_ratios (This may take a while...)'; END $$;
CREATE MATERIALIZED VIEW mv_company_financial_ratios AS 
SELECT * FROM v_company_financial_ratios
WHERE UPPER(variant) = 'TTM'
  AND publish_date IS NOT NULL;

CREATE UNIQUE INDEX idx_mv_cfr_company_variant_year_period 
ON mv_company_financial_ratios (company_id, variant, fiscal_year, fiscal_period);

CREATE INDEX idx_mv_cfr_company_id ON mv_company_financial_ratios (company_id);


-- =================================================================
-- VIEW: v_share_price_signals
-- Description: Daily technical and fundamental momentum signals
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_share_price_signals'; END $$;
CREATE VIEW v_share_price_signals AS
WITH fundamental_timeline AS (SELECT
                                 company_id,
                                 currency,
                                 fiscal_year,
                                 fiscal_period,
                                 publish_date AS valid_from,
                                 LEAD(publish_date, 1, '9999-12-31'::date) OVER (
                                     PARTITION BY company_id
                                     ORDER BY publish_date ASC, restated_date ASC
                                 ) AS valid_to,
                                 revenue,
                                 net_income,
                                 ebitda,
                                 free_cash_flow,
                                 ncav,
                                 net_net_working_capital,
                                 return_on_capital_employed,
                                 return_on_equity,
                                 return_on_assets,
                                 net_margin,
                                 gross_margin,
                                 operating_margin,
                                 fcf_margin,
                                 current_ratio,
                                 quick_ratio,
                                 debt_to_equity,
                                 interest_coverage_ratio,
                                 shares_stabilized,
                                 earnings_per_share,
                                 fcf_per_share,
                                 total_equity,
                                 total_liabilities,
                                 cash_equivalents_short_term_investments,
                                 revenue_yoy_growth,
                                 eps_yoy_growth,
                                 piotroski_f_score,
                                 revenue_growth_streak,
                                 positive_fcf_streak,
                                 positive_earnings_streak,
                                 is_cash_earnings,
                                 is_negative_equity
                              FROM mv_company_financial_ratios
                              ),
     share_price AS (SELECT
                        p.*,
                        AVG(p.close) OVER (PARTITION BY p.company_id ORDER BY p.trade_date ROWS BETWEEN 49 PRECEDING AND CURRENT ROW) AS ma_50,
                        AVG(p.close) OVER (PARTITION BY p.company_id ORDER BY p.trade_date ROWS BETWEEN 199 PRECEDING AND CURRENT ROW) AS ma_200,
                        AVG(p.volume) OVER (PARTITION BY p.company_id ORDER BY p.trade_date ROWS BETWEEN 29 PRECEDING AND CURRENT ROW) AS vol_30
                     FROM public.t_share_price p
                     ),
                     signals_base AS (SELECT
                         p.trade_date,
                         CASE
                             WHEN p.trade_date = MAX(p.trade_date) OVER (PARTITION BY p.company_id, DATE_TRUNC('month', p.trade_date)) THEN 'Y'
                             ELSE 'N'
                             END                                                         AS flag_last_day_of_month,
                         p.company_id,
                         c.company_name,
                         c.description,
                         c.sector_name,
                         c.industry_name,
                         p.ticker,
                         p.market,
                         p.currency,
                         p.close                                                         AS share_price,
                         p.volume,
                         ROUND(((p.close - p.ma_50) / NULLIF(p.ma_50, 0) * 100):: numeric,
                               2)                                                        AS pct_from_50d_ma,
                         ROUND(((p.close - p.ma_200) / NULLIF(p.ma_200, 0) * 100):: numeric,
                               2)                                                        AS pct_from_200d_ma,
                         ROUND((p.volume / NULLIF(p.vol_30, 0)):: numeric, 2)            AS volume_velocity,
                         f.fiscal_year                                                   AS active_fiscal_year,
                         f.fiscal_period                                                 AS active_fiscal_period,
                         f.valid_from                                                    AS active_report_publish_date,
                         (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) AS market_capitalization,
                         CASE 
                             WHEN (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) >= 10000000000 THEN 'LARGE'
                             WHEN (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) >= 2000000000 THEN 'MID'
                             WHEN (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) >= 300000000 THEN 'SMALL'
                             ELSE 'MICRO'
                         END                                                             AS company_size,
                         p.close / NULLIF(f.earnings_per_share, 0)                       AS pe_ratio,
                         f.earnings_per_share / NULLIF(p.close, 0)                       AS earnings_yield,
                         p.close / NULLIF(f.fcf_per_share, 0)                            AS price_to_fcf,
                         f.fcf_per_share / NULLIF(p.close, 0)                            AS fcf_yield,
                         (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
                         NULLIF(f.revenue, 0)                                            AS price_to_sales,
                         (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
                         NULLIF(f.total_equity, 0)                                       AS price_to_book,
                         (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
                         NULLIF(f.ncav, 0)                                               AS price_to_ncav,
                         (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
                         NULLIF(f.net_net_working_capital, 0)                            AS price_to_nnwc,
                         CASE
                             WHEN (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) <
                                  f.net_net_working_capital THEN 1
                             ELSE 0
                             END                                                         AS is_graham_net_net,
                         1.0 - ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
                         NULLIF(f.ncav, 0))                                              AS graham_margin_of_safety,
                         CASE
                             WHEN (p.close / NULLIF(f.earnings_per_share, 0)) < 15 
                                  AND ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) / NULLIF(f.total_equity, 0)) < 1.5 
                                  AND f.current_ratio > 2.0 
                                  AND f.positive_earnings_streak >= 4 THEN 1
                             ELSE 0
                             END                                                         AS is_graham_defensive,
                         ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) +
                          COALESCE(f.total_liabilities, 0) -
                          COALESCE(f.cash_equivalents_short_term_investments, 0))        AS enterprise_value,
                         ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) +
                          COALESCE(f.total_liabilities, 0) -
                          COALESCE(f.cash_equivalents_short_term_investments, 0))
                             /
                         NULLIF(f.ebitda, 0)                                             AS enterprise_value_to_ebitda,
                         ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) +
                          COALESCE(f.total_liabilities, 0) -
                          COALESCE(f.cash_equivalents_short_term_investments, 0))
                             /
                         NULLIF(f.revenue, 0)                                            AS enterprise_value_to_revenue,
                         ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) +
                          COALESCE(f.total_liabilities, 0) -
                          COALESCE(f.cash_equivalents_short_term_investments, 0))
                             /
                         NULLIF(f.free_cash_flow, 0)                                     AS enterprise_value_to_free_cash_flow,
                         f.return_on_capital_employed,
                         f.return_on_equity,
                         f.return_on_assets,
                         f.net_margin,
                         f.gross_margin,
                         f.operating_margin,
                         f.fcf_margin,
                         f.current_ratio,
                         f.quick_ratio,
                         f.debt_to_equity,
                         f.interest_coverage_ratio,
                         f.revenue_yoy_growth,
                         f.eps_yoy_growth,
                         f.piotroski_f_score,
                         f.revenue_growth_streak,
                         f.positive_fcf_streak,
                         f.positive_earnings_streak,
                         f.is_cash_earnings,
                         f.is_negative_equity,
                         (CASE WHEN f.return_on_equity > 0.15 AND f.return_on_capital_employed > 0.12 THEN 10
                               WHEN f.return_on_equity > 0.08 OR f.return_on_capital_employed > 0.08 THEN 5
                               ELSE 0 END) * 2.5 +
                         (CASE WHEN f.free_cash_flow > 0 AND f.fcf_margin > 0.08 AND f.is_cash_earnings = 1 THEN 10
                               WHEN f.free_cash_flow > 0 THEN 5
                               ELSE 0 END) * 2.0 +
                         (CASE WHEN f.debt_to_equity < 0.5 AND f.current_ratio > 2.0 AND f.interest_coverage_ratio > 10 THEN 10
                               WHEN f.debt_to_equity < 1.5 AND f.current_ratio > 1.2 THEN 5
                               ELSE 0 END) * 2.0 +
                         (CASE WHEN f.revenue_yoy_growth > 0.10 AND f.eps_yoy_growth > 0.10 THEN 10
                               WHEN f.revenue_yoy_growth > 0 THEN 5
                               ELSE 0 END) * 2.0 +
                         (CASE WHEN f.piotroski_f_score >= 7 AND f.revenue_growth_streak >= 3 THEN 10
                               WHEN f.piotroski_f_score >= 4 THEN 5
                               ELSE 0 END) * 1.5                                         AS quality_score
                      FROM share_price p
                      LEFT JOIN t_company c ON c.id = p.company_id
                      LEFT JOIN fundamental_timeline f ON p.company_id = f.company_id
                                                      AND p.trade_date >= f.valid_from
                                                      AND p.trade_date < f.valid_to
                      )
SELECT
    *,
    CASE
       WHEN quality_score >= 70
            AND (earnings_yield > 0.06 OR fcf_yield > 0.06)
            AND piotroski_f_score >= 7        THEN 'STRONG_BUY'
       WHEN quality_score >= 50
            AND (earnings_yield > 0.04 OR fcf_yield > 0.04)
            AND piotroski_f_score >= 5        THEN 'BUY'
       WHEN piotroski_f_score <= 2
            OR quality_score < 30               THEN 'AVOID'
       ELSE 'HOLD'
       END                                                         AS composite_signal
FROM signals_base;

-- MATERIALIZE Recent Share Price Signals
DO $$ BEGIN RAISE NOTICE 'Materializing view: mv_share_price_signals_recent (Caching last 30 days...)'; END $$;
CREATE MATERIALIZED VIEW mv_share_price_signals_recent AS 
SELECT * FROM v_share_price_signals 
WHERE trade_date >= CURRENT_DATE - INTERVAL '30 days';

CREATE UNIQUE INDEX idx_mv_sps_company_trade_date 
ON mv_share_price_signals_recent (company_id, trade_date);

CREATE INDEX idx_mv_sps_company_id ON mv_share_price_signals_recent (company_id);

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
        WHEN news_sentiment_30d <= -0.35 THEN 'BEARISH'
        WHEN news_sentiment_30d <= -0.15 THEN 'SOMEWHAT_BEARISH'
        WHEN news_sentiment_30d < 0.15 THEN 'NEUTRAL'
        WHEN news_sentiment_30d < 0.35 THEN 'SOMEWHAT_BULLISH'
        ELSE 'BULLISH'
    END as news_sentiment_label_30d

FROM sentiment_calc;

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
       RANK() OVER (ORDER BY s.piotroski_f_score DESC NULLS LAST) AS fscore_rank
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
  AND s.piotroski_f_score >= 5;

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
  AND s.piotroski_f_score >= 6;

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
-- VIEW: v_screener_piotroski
-- Description: Fundamental Health strategy
-- =================================================================
CREATE VIEW v_screener_piotroski AS
SELECT s.*
FROM mv_share_price_signals_recent s
WHERE s.trade_date = (SELECT MAX(trade_date) FROM mv_share_price_signals_recent WHERE company_id = s.company_id)
  AND s.piotroski_f_score >= 7;
