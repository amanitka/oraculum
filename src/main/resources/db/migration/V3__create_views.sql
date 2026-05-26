-- Flyway migration script for creating database views

-- =================================================================
-- VIEW: v_derived_metrics
-- Description: [Please add a brief description of the view]
-- =================================================================
CREATE OR REPLACE VIEW v_derived_metrics AS
WITH statement_values AS (SELECT
                             income.composite_key,
					         income.ticker,
					         income.simfin_id,
					         income.currency,
					         income.template,
					         income.variant,
					         income.fiscal_year,
					         income.fiscal_period,
					         income.report_date,
					         income.publish_date,
					         income.restated_date,
					         NULLIF(income.payload ->> 'Revenue'::text, ''::text)::double precision AS revenue,
						     NULLIF(income.payload ->> 'Cost of Revenue'::text, ''::text)::double precision AS cost_of_revenue,
						     NULLIF(income.payload ->> 'Net Income'::text, ''::text)::double precision AS net_income,
						     NULLIF(income.payload ->> 'Operating Income (Loss)'::text, ''::text)::double precision AS operating_income,
						     NULLIF(income.payload ->> 'Interest Expense, Net'::text, ''::text)::double precision AS interest_expense_net,
						     NULLIF(income.payload ->> 'Income Tax (Expense) Benefit, Net'::text, ''::text)::double precision AS income_tax,
						     NULLIF(income.payload ->> 'Depreciation & Amortization'::text, ''::text)::double precision AS depreciation_amortization,
						     NULLIF(balance.payload ->> 'Total Equity'::text, ''::text)::double precision AS total_equity,
						     NULLIF(balance.payload ->> 'Total Current Assets'::text, ''::text)::double precision AS total_current_assets,
						     NULLIF(balance.payload ->> 'Total Current Liabilities'::text, ''::text)::double precision AS total_current_liabilities,
						     NULLIF(balance.payload ->> 'Total Liabilities'::text, ''::text)::double precision AS total_liabilities,
						     NULLIF(balance.payload ->> 'Cash, Cash Equivalents & Short Term Investments'::text, ''::text)::double precision AS cash_equivalents_short_term_investments,
						     NULLIF(balance.payload ->> 'Accounts & Notes Receivable'::text, ''::text)::double precision AS accounts_notes_receivable,
						     NULLIF(balance.payload ->> 'Inventories'::text, ''::text)::double precision AS inventories,
						     NULLIF(income.payload ->> 'Shares (Basic)'::text, ''::text)::double precision AS shares_basic,
						     NULLIF(income.payload ->> 'Shares (Diluted)'::text, ''::text)::double precision AS shares_diluted,
						     NULLIF(cash_flow.payload ->> 'Net Cash from Operating Activities'::text, ''::text)::double precision AS net_cash_from_operating_activities,
						     NULLIF(cash_flow.payload ->> 'Change in Fixed Assets & Intangibles'::text, ''::text)::double precision AS capital_expenditures
					      FROM t_income_statement income
					      LEFT JOIN t_balance_sheet balance ON balance.ticker = income.ticker
					                                       AND balance.simfin_id = income.simfin_id
					                                    	AND balance.currency = income.currency
					                                    	AND balance.template = income.template
					                                    	AND balance.variant = income.variant
					                                    	AND balance.fiscal_year = income.fiscal_year
					                                    	AND CASE WHEN balance.variant = 'annual' THEN 'FY' ELSE balance.fiscal_period END = income.fiscal_period
					      LEFT JOIN t_cash_flow_statement cash_flow ON cash_flow.ticker = income.ticker
					                                               AND cash_flow.simfin_id = income.simfin_id
					                                               AND cash_flow.currency = income.currency
					                                               AND cash_flow.template = income.template
					                                               AND cash_flow.variant = income.variant
					                                               AND cash_flow.fiscal_year = income.fiscal_year
					                                               AND cash_flow.fiscal_period = income.fiscal_period
					      )
SELECT
    composite_key,
    ticker,
    simfin_id,
    currency,
    template,
    variant,
    fiscal_year,
    fiscal_period,
    report_date,
    publish_date,
    restated_date,
    -- =========================================================================
    -- 1. VALUATION & CASH METRICS
    -- =========================================================================
    -- EBITDA (Sign protected)
    COALESCE(operating_income, 0::double precision) + ABS(COALESCE(depreciation_amortization, 0::double precision)) AS ebitda,
    -- FREE CASH FLOW
    COALESCE(net_cash_from_operating_activities, 0::double precision) + COALESCE(capital_expenditures, 0::double precision) AS free_cash_flow,
    -- NCAV (Net Current Asset Value)
    COALESCE(total_current_assets, 0::double precision) - ABS(COALESCE(total_liabilities, 0::double precision)) AS ncav,
    -- NNWC (Net-Net Working Capital)
    COALESCE(cash_equivalents_short_term_investments, 0::double precision)
        + (COALESCE(accounts_notes_receivable, 0::double precision) * 0.75::double precision)
        + (COALESCE(inventories, 0::double precision) * 0.5::double precision)
        - ABS(COALESCE(total_liabilities, 0::double precision)) AS net_net_working_capital,
    -- =========================================================================
    -- 2. CAPITAL EFFICIENCY & MARGIN METRICS
    -- =========================================================================
    -- ROCE (Return on Capital Employed) - Total Assets proxy: (Equity + Liabilities)
    operating_income / NULLIF((total_equity + ABS(COALESCE(total_liabilities, 0::double precision))), 0::double precision) AS return_on_capital_employed,
    -- ROE (Return on Equity)
    net_income / NULLIF(total_equity, 0::double precision) AS return_on_equity,
    -- ROA (Return on Assets)
    net_income / NULLIF((total_equity + ABS(COALESCE(total_liabilities, 0::double precision))), 0::double precision) AS return_on_assets,
    -- NET MARGIN
    net_income / NULLIF(revenue, 0::double precision) AS net_margin,
    -- =========================================================================
    -- 3. SOLVENCY, LIQUIDITY & EFFICIENCY METRICS
    -- =========================================================================
    -- CURRENT RATIO
    total_current_assets / NULLIF(ABS(COALESCE(total_current_liabilities, 0::double precision)), 0::double precision) AS current_ratio,
    -- DEBT TO EQUITY
    ABS(COALESCE(total_liabilities, 0::double precision)) / NULLIF(total_equity, 0::double precision) AS debt_to_equity,
    -- INVENTORY TURNOVER (Forces negative cost_of_revenue to absolute positive)
    ABS(COALESCE(cost_of_revenue, 0::double precision)) / NULLIF(inventories, 0::double precision) AS inventory_turnover,
    -- ASSET TURNOVER
    revenue / NULLIF((total_equity + ABS(COALESCE(total_liabilities, 0::double precision))), 0::double precision) AS asset_turnover,
    -- =========================================================================
    -- 4. PER SHARE & RAW BASE DATA FIELDS
    -- =========================================================================
    COALESCE(shares_diluted, shares_basic) AS shares_stabilized,
    -- EPS (Earnings Per Share)
    net_income / NULLIF(COALESCE(shares_diluted, shares_basic), 0::double precision) AS earnings_per_share,
    -- FCF Per Share
    (COALESCE(net_cash_from_operating_activities, 0::double precision) + COALESCE(capital_expenditures, 0::double precision))
        / NULLIF(COALESCE(shares_diluted, shares_basic), 0::double precision) AS fcf_per_share,
    revenue,
    net_income
FROM statement_values;

-- =================================================================
-- VIEW: v_daily_market_signals
-- Description: [Please add a brief description of the view]
-- =================================================================
CREATE OR REPLACE VIEW v_daily_market_signals AS
WITH fundamental_timeline AS (SELECT
							     ticker,
							     simfin_id,
							     currency,
							     fiscal_year,
							     fiscal_period,
							     publish_date AS valid_from,
							     LEAD(publish_date, 1, '9999-12-31'::date) OVER (
							         PARTITION BY ticker
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
							     current_ratio,
							     debt_to_equity,
							     shares_stabilized,
							     earnings_per_share,
							     fcf_per_share,
							     -- Pulling base variables directly to avoid division hacks in the daily outer select
							     (net_income / NULLIF(return_on_equity, 0)) AS derived_total_equity,
							     (revenue / NULLIF(asset_turnover, 0)) AS derived_total_assets
							  FROM v_derived_metrics
							  WHERE variant = 'ttm'
							    AND publish_date IS NOT NULL
                              ),
     share_price AS (SELECT
					    p.*,
					    AVG(p.close) OVER (PARTITION BY p.ticker ORDER BY p.trade_date ROWS BETWEEN 49 PRECEDING AND CURRENT ROW) AS ma_50,
					    AVG(p.close) OVER (PARTITION BY p.ticker ORDER BY p.trade_date ROWS BETWEEN 199 PRECEDING AND CURRENT ROW) AS ma_200,
					    AVG(p.volume) OVER (PARTITION BY p.ticker ORDER BY p.trade_date ROWS BETWEEN 29 PRECEDING AND CURRENT ROW) AS vol_30
					 FROM public.t_share_price p
                     )
SELECT
   -- Context Keys
   p.trade_date,
   CASE
       WHEN p.trade_date = MAX(p.trade_date) OVER (PARTITION BY p.ticker, DATE_TRUNC('month', p.trade_date)) THEN 'Y'
       ELSE 'N'
       END                                                         AS flag_last_day_of_month,
   p.ticker,
   p.market,
   p.currency,
   -- Core Market Pricing & Technical Momentum
   p.close                                                         AS share_price,
   p.volume,
   ROUND(((p.close - p.ma_50) / NULLIF(p.ma_50, 0) * 100):: numeric,
         2)                                                        AS pct_from_50d_ma,
   ROUND(((p.close - p.ma_200) / NULLIF(p.ma_200, 0) * 100):: numeric,
         2)                                                        AS pct_from_200d_ma,
   ROUND((p.volume / NULLIF(p.vol_30, 0)):: numeric, 2)            AS volume_velocity,
   -- Fundamental Context
   f.fiscal_year                                                   AS active_fiscal_year,
   f.fiscal_period                                                 AS active_fiscal_period,
   f.valid_from                                                    AS active_report_publish_date,
   -- 1. Valuation & Size Metrics
   (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) AS market_capitalization,
   p.close / NULLIF(f.earnings_per_share, 0)                       AS pe_ratio,
   f.earnings_per_share / NULLIF(p.close, 0)                       AS earnings_yield,
   p.close / NULLIF(f.fcf_per_share, 0)                            AS price_to_fcf,
   (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
   NULLIF(f.revenue, 0)                                            AS price_to_sales,
   (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
   NULLIF(f.derived_total_equity, 0)                               AS price_to_book,
   -- 2. Deep Value Graham Signals
   (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
   NULLIF(f.ncav, 0)                                               AS price_to_ncav,
   (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) /
   NULLIF(f.net_net_working_capital, 0)                            AS price_to_nnwc,
   CASE
       WHEN (p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) <
            f.net_net_working_capital THEN 1
       ELSE 0
       END                                                         AS is_graham_net_net,
   -- 3. Enterprise Value (EV) Multiples (Corrected: Market Cap + Total Liabilities)
   -- Total Liabilities is computed as: Total Assets - Total Equity
   ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) +
    (f.derived_total_assets - f.derived_total_equity))             AS enterprise_value,
   ((p.close * COALESCE(p.shares_outstanding, f.shares_stabilized)) +
    (f.derived_total_assets - f.derived_total_equity))
       /
   NULLIF(f.ebitda, 0)                                             AS enterprise_value_to_ebitda,
   -- 4. Capital Efficiency
   f.return_on_capital_employed,
   f.return_on_equity,
   f.net_margin,
   -- 5. Solvency & Liquidity
   f.current_ratio,
   f.debt_to_equity
FROM share_price p
LEFT JOIN fundamental_timeline f ON p.ticker = f.ticker
                                AND p.trade_date >= f.valid_from
                                AND p.trade_date < f.valid_to;