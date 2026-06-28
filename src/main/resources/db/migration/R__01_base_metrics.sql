-- Flyway repeatable migration script for creating/replacing core views
-- Description: Core views containing corrected logic and advanced financial metrics, utilizing materialized views for performance

DROP MATERIALIZED VIEW IF EXISTS mv_company_financial_ratios CASCADE;
DROP VIEW IF EXISTS v_company_financial_ratios CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_share_price_signals_recent CASCADE;
DROP VIEW IF EXISTS v_share_price_signals CASCADE;
DROP VIEW IF EXISTS v_industry_financial_ratios CASCADE;

-- =================================================================
-- VIEW: v_company_financial_ratios
-- Description: Core derived metrics for fundamental analysis
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_company_financial_ratios'; END $$;
CREATE VIEW v_company_financial_ratios AS
WITH raw_data AS (SELECT
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
                     NULLIF(income.statement_data ->> 'Revenue'::text,                                          ''::text)::double precision AS revenue,
                     NULLIF(income.statement_data ->> 'Cost of Revenue'::text,                                  ''::text)::double precision AS cost_of_revenue,
                     NULLIF(income.statement_data ->> 'Net Income'::text,                                       ''::text)::double precision AS net_income,
                     NULLIF(income.statement_data ->> 'Operating Income (Loss)'::text,                          ''::text)::double precision AS operating_income,
                     NULLIF(income.statement_data ->> 'Interest Expense, Net'::text,                            ''::text)::double precision AS interest_expense_net,
                     NULLIF(cash_flow.statement_data ->> 'Depreciation & Amortization'::text,                   ''::text)::double precision AS da,
                     NULLIF(balance.statement_data ->> 'Total Equity'::text,                                    ''::text)::double precision AS equity,
                     NULLIF(balance.statement_data ->> 'Total Assets'::text,                                    ''::text)::double precision AS assets,
                     NULLIF(balance.statement_data ->> 'Total Current Assets'::text,                            ''::text)::double precision AS cur_assets,
                     NULLIF(balance.statement_data ->> 'Total Current Liabilities'::text,                       ''::text)::double precision AS cur_liabilities,
                     NULLIF(balance.statement_data ->> 'Total Liabilities'::text,                               ''::text)::double precision AS liabilities,
                     NULLIF(balance.statement_data ->> 'Cash, Cash Equivalents & Short Term Investments'::text, ''::text)::double precision AS cash,
                     NULLIF(balance.statement_data ->> 'Accounts & Notes Receivable'::text,                     ''::text)::double precision AS receivables,
                     NULLIF(balance.statement_data ->> 'Inventories'::text,                                     ''::text)::double precision AS inventories,
                     NULLIF(income.statement_data ->> 'Shares (Basic)'::text,                                   ''::text)::double precision AS shares_basic,
                     NULLIF(income.statement_data ->> 'Shares (Diluted)'::text,                                 ''::text)::double precision AS shares_diluted,
                     NULLIF(cash_flow.statement_data ->> 'Net Cash from Operating Activities'::text,            ''::text)::double precision AS ncf_ops,
                     NULLIF(cash_flow.statement_data ->> 'Change in Fixed Assets & Intangibles'::text,          ''::text)::double precision AS capex
                  FROM t_income_statement income
                  LEFT JOIN t_balance_sheet balance ON balance.company_id  = income.company_id
                                                   AND balance.currency    = income.currency
                                                   AND balance.template    = income.template
                                                   AND balance.variant     = income.variant
                                                   AND balance.fiscal_year = income.fiscal_year
                                                   AND CASE WHEN UPPER(balance.variant) = 'ANNUAL' THEN 'FY' ELSE balance.fiscal_period END = income.fiscal_period
                  LEFT JOIN t_cash_flow_statement cash_flow ON cash_flow.company_id  = income.company_id
                                                           AND cash_flow.currency    = income.currency
                                                           AND cash_flow.template    = income.template
                                                           AND cash_flow.variant     = income.variant
                                                           AND cash_flow.fiscal_year = income.fiscal_year
                                                           AND CASE WHEN UPPER(cash_flow.variant) = 'ANNUAL' THEN 'FY' ELSE cash_flow.fiscal_period END = income.fiscal_period
                  ),
     -- =================================================================
     -- =================================================================
     -- Normalization: resolve sign conventions once.
     --
     -- SimFin sign conventions (verified against full dataset, 239,414 rows):
     --   liabilities      : stored as POSITIVE  (239,255 / 239,414 rows)
     --   cur_liabilities  : stored as POSITIVE
     --   capex            : stored as NEGATIVE   (cash outflow) → ABS() mandatory
     --   D&A              : stored as POSITIVE   → ABS() redundant but kept as guard
     --   interest_exp_net : stored as NEGATIVE when it is a net cost
     --
     -- NOTE on ABS(liabilities): 66 / 239,414 rows have negative liabilities
     -- (SimFin data anomalies, not sign convention). Without ABS() those rows
     -- would compute NCAV as cur_assets + liabilities, silently inflating the
     -- value. ABS() is intentional normalization here, not defensive redundancy.
     -- =================================================================
     normalized AS (SELECT
                       *,
                       COALESCE(operating_income, 0) + ABS(COALESCE(da, 0))                                                                     AS ebitda,
                       COALESCE(ncf_ops, 0)          - ABS(COALESCE(capex, 0))                                                                  AS fcf,  -- capex is negative in SimFin → ABS() required
                       COALESCE(cur_assets, 0)       - ABS(COALESCE(liabilities, 0))                                                            AS ncav,  -- ABS() guards 66 anomalous negative-liability rows
                       COALESCE(cash, 0) + (COALESCE(receivables, 0) * 0.75) + (COALESCE(inventories, 0) * 0.5) - ABS(COALESCE(liabilities, 0)) AS nnwc,  -- same guard
                       COALESCE(shares_diluted, shares_basic)                                                                                   AS shares
                    FROM raw_data
                    ),
     computed_ratios AS (SELECT
                            *,
                            operating_income / NULLIF(COALESCE(assets, equity + ABS(COALESCE(liabilities, 0))), 0)  AS roce,
                            net_income       / NULLIF(equity, 0)                                                     AS roe,
                            net_income       / NULLIF(assets, 0)                                                     AS roa,
                            net_income       / NULLIF(revenue, 0)                                                    AS net_margin,
                            (revenue - ABS(COALESCE(cost_of_revenue, 0))) / NULLIF(revenue, 0)                       AS gross_margin,
                            operating_income / NULLIF(revenue, 0)                                                    AS op_margin,
                            fcf              / NULLIF(revenue, 0)                                                    AS fcf_margin,
                            cur_assets       / NULLIF(ABS(COALESCE(cur_liabilities, 0)), 0)                          AS current_ratio,
                            (COALESCE(cash, 0) + COALESCE(receivables, 0)) / NULLIF(ABS(COALESCE(cur_liabilities, 0)), 0) AS quick_ratio,
                            ABS(COALESCE(liabilities, 0)) / NULLIF(equity, 0)                                        AS debt_to_equity,
                            CASE WHEN ABS(COALESCE(interest_expense_net, 0)) > 0
                                 THEN operating_income / ABS(interest_expense_net)
                                 ELSE NULL
                            END                                                                                       AS int_coverage,
                            ABS(COALESCE(cost_of_revenue, 0)) / NULLIF(inventories, 0)                               AS inv_turnover,
                            revenue          / NULLIF(assets, 0)                                                     AS asset_turnover,
                            net_income       / NULLIF(shares, 0)                                                     AS eps,
                            fcf              / NULLIF(shares, 0)                                                     AS fcf_per_share
                         FROM normalized
                         ),
     -- =================================================================
     -- Two windows are intentionally used:
     --   w_yoy: same fiscal_period, prior year  → true YoY growth
     --   w_seq: consecutive periods             → Piotroski trend signals
     -- =================================================================
     lagged AS (SELECT
                   *,
                   LAG(revenue)        OVER w_yoy AS prev_rev,
                   LAG(net_income)     OVER w_yoy AS prev_ni,
                   LAG(ebitda)         OVER w_yoy AS prev_ebitda,
                   LAG(fcf)            OVER w_yoy AS prev_fcf,
                   LAG(eps)            OVER w_yoy AS prev_eps,
                   LAG(roa)            OVER w_seq AS prev_roa,
                   LAG(debt_to_equity) OVER w_seq AS prev_d2e,
                   LAG(current_ratio)  OVER w_seq AS prev_cr,
                   LAG(shares)         OVER w_seq AS prev_shares,
                   LAG(gross_margin)   OVER w_seq AS prev_gm,
                   LAG(asset_turnover) OVER w_seq AS prev_at,
                   LAG(op_margin)      OVER w_seq AS prev_om,
                   LAG(net_margin)     OVER w_seq AS prev_nm
                FROM computed_ratios
                WINDOW w_yoy AS (PARTITION BY company_id, fiscal_period, UPPER(variant) ORDER BY fiscal_year),
                       w_seq AS (PARTITION BY company_id,                UPPER(variant) ORDER BY fiscal_year, fiscal_period)
                ),
     streaks AS (SELECT
                    *,
                    SUM(CASE WHEN fcf        > 0        THEN 1 ELSE 0 END) OVER w4     AS fcf_streak,
                    SUM(CASE WHEN net_income > 0        THEN 1 ELSE 0 END) OVER w4     AS ni_streak,
                    SUM(CASE WHEN revenue    > prev_rev THEN 1 ELSE 0 END) OVER w4_yoy AS rev_growth_streak
                 FROM lagged
                 WINDOW w4     AS (PARTITION BY company_id,               UPPER(variant) ORDER BY fiscal_year, fiscal_period ROWS BETWEEN 3 PRECEDING AND CURRENT ROW),
                        w4_yoy AS (PARTITION BY company_id, fiscal_period, UPPER(variant) ORDER BY fiscal_year              ROWS BETWEEN 3 PRECEDING AND CURRENT ROW)
                 ),
     final_metrics AS (SELECT
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
                          revenue,
                          net_income,
                          equity       AS total_equity,
                          liabilities  AS total_liabilities,
                          assets       AS total_assets,
                          cash         AS cash_equivalents_short_term_investments,
                          ncf_ops      AS net_cash_from_operating_activities,
                          ebitda,
                          fcf          AS free_cash_flow,
                          ncav,
                          nnwc         AS net_net_working_capital,
                          shares       AS shares_stabilized,
                          eps          AS earnings_per_share,
                          fcf_per_share,
                          roce         AS return_on_capital_employed,
                          roe          AS return_on_equity,
                          roa          AS return_on_assets,
                          net_margin,
                          gross_margin,
                          op_margin    AS operating_margin,
                          fcf_margin,
                          current_ratio,
                          quick_ratio,
                          debt_to_equity,
                          int_coverage      AS interest_coverage_ratio,
                          inv_turnover      AS inventory_turnover,
                          asset_turnover,
                          rev_growth_streak AS revenue_growth_streak,
                          fcf_streak        AS positive_fcf_streak,
                          ni_streak         AS positive_earnings_streak,
                          (revenue    - prev_rev)    / NULLIF(ABS(prev_rev),    0) AS revenue_yoy_growth,
                          (net_income - prev_ni)     / NULLIF(ABS(prev_ni),     0) AS net_income_yoy_growth,
                          (ebitda     - prev_ebitda) / NULLIF(ABS(prev_ebitda), 0) AS ebitda_yoy_growth,
                          (fcf        - prev_fcf)    / NULLIF(ABS(prev_fcf),    0) AS fcf_yoy_growth,
                          (eps        - prev_eps)    / NULLIF(ABS(prev_eps),    0) AS eps_yoy_growth,
                          (CASE WHEN roa          >  0            THEN 1 ELSE 0 END) +
                          (CASE WHEN ncf_ops      >  0            THEN 1 ELSE 0 END) +
                          (CASE WHEN roa          >  prev_roa     THEN 1 ELSE 0 END) +
                          (CASE WHEN ncf_ops      >  net_income   THEN 1 ELSE 0 END) +
                          (CASE WHEN debt_to_equity < prev_d2e    THEN 1 ELSE 0 END) +
                          (CASE WHEN current_ratio  >  prev_cr    THEN 1 ELSE 0 END) +
                          (CASE WHEN shares         <= prev_shares THEN 1 ELSE 0 END) +
                          (CASE WHEN gross_margin   >  prev_gm    THEN 1 ELSE 0 END) +
                          (CASE WHEN asset_turnover >  prev_at    THEN 1 ELSE 0 END)  AS financial_trend_score,
                          ncf_ops / NULLIF(net_income, 0)                             AS earnings_quality_ratio,
                          (CASE WHEN ncf_ops    >  net_income THEN 1 ELSE 0 END)      AS is_cash_earnings,
                          (CASE WHEN equity     <  0          THEN 1 ELSE 0 END)      AS is_negative_equity,
                          (CASE WHEN gross_margin > prev_gm
                                AND op_margin    > prev_om
                                AND net_margin   > prev_nm   THEN 1 ELSE 0 END)       AS margin_expansion_signal
                       FROM streaks
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
    revenue,
    net_income,
    total_equity,
    total_liabilities,
    total_assets,
    cash_equivalents_short_term_investments,
    net_cash_from_operating_activities,
    ebitda,
    free_cash_flow,
    ncav,
    net_net_working_capital,
    shares_stabilized,
    earnings_per_share,
    fcf_per_share,
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
    revenue_growth_streak,
    positive_fcf_streak,
    positive_earnings_streak,
    revenue_yoy_growth,
    net_income_yoy_growth,
    ebitda_yoy_growth,
    fcf_yoy_growth,
    eps_yoy_growth,
    financial_trend_score,
    earnings_quality_ratio,
    is_cash_earnings,
    is_negative_equity,
    margin_expansion_signal,
    (CASE WHEN return_on_equity > 0.15 AND return_on_capital_employed > 0.12 THEN 10
          WHEN return_on_equity > 0.08 OR  return_on_capital_employed > 0.08 THEN 5
          ELSE 0 END) * 2.5 +
    (CASE WHEN free_cash_flow > 0 AND fcf_margin > 0.08 AND is_cash_earnings = 1 THEN 10
          WHEN free_cash_flow > 0 THEN 5
          ELSE 0 END) * 2.0 +
    (CASE WHEN debt_to_equity < 0.5 AND current_ratio > 2.0 AND interest_coverage_ratio > 10 THEN 10
          WHEN debt_to_equity < 1.5 AND current_ratio > 1.2 THEN 5
          ELSE 0 END) * 2.0 +
    (CASE WHEN COALESCE(revenue_yoy_growth, 0) > 0.10
           AND COALESCE(eps_yoy_growth,     0) > 0.10 THEN 10
          WHEN COALESCE(revenue_yoy_growth, 0) > 0 THEN 5
          ELSE 0 END) * 2.0 +
    (CASE WHEN financial_trend_score >= 7 AND revenue_growth_streak >= 3 THEN 10
          WHEN financial_trend_score >= 4 THEN 5
          ELSE 0 END) * 1.5 AS quality_score
FROM final_metrics;

-- MATERIALIZE Fundamental Ratios
DO $$ BEGIN RAISE NOTICE 'Materializing view: mv_company_financial_ratios (This may take a while...)'; END $$;
CREATE MATERIALIZED VIEW mv_company_financial_ratios AS 
SELECT * FROM v_company_financial_ratios
WHERE publish_date IS NOT NULL;

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
                                 financial_trend_score,
                                 quality_score,
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
                         f.financial_trend_score,
                         f.revenue_growth_streak,
                         f.positive_fcf_streak,
                         f.positive_earnings_streak,
                         f.is_cash_earnings,
                         f.is_negative_equity,
                         f.quality_score
                      FROM share_price p
                      LEFT JOIN t_company c ON c.id = p.company_id
                      LEFT JOIN fundamental_timeline f ON p.company_id = f.company_id
                                                      AND p.trade_date >= f.valid_from
                                                      AND p.trade_date <  f.valid_to
                      )
SELECT
    *,
    CASE
       WHEN quality_score >= 70
            AND (earnings_yield > 0.06 OR fcf_yield > 0.06)
            AND financial_trend_score >= 7        THEN 'STRONG_BUY'
       WHEN quality_score >= 50
            AND (earnings_yield > 0.04 OR fcf_yield > 0.04)
            AND financial_trend_score >= 5        THEN 'BUY'
       WHEN financial_trend_score <= 2
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
-- VIEW: v_industry_financial_ratios
-- Description: Aggregates current cross-sectional financial ratios by industry for peer comparison
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_industry_financial_ratios'; END $$;
CREATE VIEW v_industry_financial_ratios AS
WITH latest_ratios AS (
    SELECT DISTINCT ON (company_id, variant) *
    FROM mv_company_financial_ratios
    ORDER BY company_id, variant, report_date DESC
)
SELECT
    c.industry_name,
    r.variant,
    COUNT(r.company_id) AS company_count,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.return_on_equity) AS return_on_equity,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.gross_margin) AS gross_margin,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.net_margin) AS net_margin,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.debt_to_equity) AS debt_to_equity,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.current_ratio) AS current_ratio,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.operating_margin) AS operating_margin,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.fcf_margin) AS fcf_margin,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY r.revenue_yoy_growth) AS revenue_yoy_growth
FROM latest_ratios r
JOIN t_company c ON c.id = r.company_id
WHERE c.industry_name IS NOT NULL
GROUP BY c.industry_name, r.variant;

