-- =================================================================
-- VIEW: v_company_institutional_flow
-- Description: Institutional 13F ownership summary and flows per company.
--              Enriched with dynamic quarter indexing (1 = latest quarter)
--              and percentage shares flow for UI heatmaps and LLM analysis.
-- =================================================================
DROP VIEW IF EXISTS public.v_company_institutional_flow CASCADE;

DO $$ BEGIN RAISE NOTICE 'Creating view: v_company_institutional_flow'; END $$;

CREATE VIEW public.v_company_institutional_flow AS
SELECT 
   c.id                                        AS company_id,
   c.ticker,
   c.company_name,
   c.market,
   c.sector_name,
   c.industry_name,
   s.cusip,
   s.report_period,
   -- Dynamic quarter index: 1 = latest quarter, 2 = 1Q ago, 3 = 2Q ago...
   ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY s.report_period DESC) AS quarter_index,
   -- Portfolio holdings & concentration
   s.total_inst_value_usd,
   s.total_inst_shares,
   s.total_holders_count,
   s.top_holders_names,
   s.top5_holders_value_usd,
   s.top5_holders_pct,
   -- Institutional Options & Sentiment
   s.put_value_usd,
   s.call_value_usd,
   s.put_call_ratio,
   -- Net flows & percentage changes
   s.net_value_change_usd,
   s.net_shares_change,
   ROUND(
     CASE 
       WHEN (s.total_inst_shares - s.net_shares_change) <= 0 AND s.net_shares_change > 0 THEN 100.00
       WHEN (s.total_inst_shares - s.net_shares_change) <= 0 AND s.net_shares_change < 0 THEN -100.00
       ELSE LEAST(GREATEST(s.net_shares_change * 100.0 / NULLIF(s.total_inst_shares - s.net_shares_change, 0), -100.00), 9999.00)
     END, 2) AS shares_change_pct,
   -- Institutional breadth
   s.funds_buying_count,
   s.funds_selling_count,
   s.funds_new_count,
   s.funds_closed_count,
   s.funds_unchanged_count,
   -- Smart Money (Tier 1 Superinvestors)
   s.tier1_value_usd,
   s.tier1_shares,
   s.tier1_holders_count,
   s.tier1_net_value_change_usd,
   s.tier1_net_shares_change,
   s.tier1_buyers_count,
   s.tier1_sellers_count,
   s.tier1_new_count,
   s.tier1_closed_count,
   s.tier1_buyer_names,
   s.tier1_seller_names
FROM public.t_company c
JOIN public.t_sec_holding_summary s ON s.cusip = c.cusip
WHERE c.cusip IS NOT NULL;

-- =================================================================
-- VIEW: v_sector_institutional_flow
-- Description: Aggregated quarterly institutional ownership and flows by sector.
--              Built on top of v_company_institutional_flow.
-- =================================================================
DROP VIEW IF EXISTS public.v_sector_institutional_flow CASCADE;

DO $$ BEGIN RAISE NOTICE 'Creating view: v_sector_institutional_flow'; END $$;

CREATE VIEW public.v_sector_institutional_flow AS
SELECT 
   f.sector_name,
   f.report_period,
   COALESCE(SUM(f.total_inst_value_usd), 0)::BIGINT AS total_inst_value_usd,
   COALESCE(SUM(f.net_value_change_usd), 0)::BIGINT AS net_value_change_usd,
   COALESCE(SUM(f.total_inst_shares), 0)::BIGINT    AS total_inst_shares,
   COALESCE(SUM(f.net_shares_change), 0)::BIGINT    AS net_shares_change,
   COUNT(DISTINCT f.company_id)::BIGINT             AS companies_count
FROM public.v_company_institutional_flow f
WHERE f.sector_name IS NOT NULL AND f.sector_name <> ''
GROUP BY f.sector_name, f.report_period;

