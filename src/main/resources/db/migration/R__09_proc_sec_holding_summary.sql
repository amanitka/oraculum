-- =================================================================
-- REPEATABLE: R__09_proc_sec_holding_summary.sql
-- Description: Stored procedure for incremental recalculation of
--              t_sec_holding_summary using PostgreSQL 18 MERGE
--              and fn_sec_holding_delta.
-- =================================================================

CREATE OR REPLACE PROCEDURE public.recalculate_sec_holding_summary(p_report_period DATE)
LANGUAGE plpgsql AS $$
BEGIN
  MERGE INTO t_sec_holding_summary AS target
  USING (WITH ranked_holdings AS (SELECT
                                     h.cik,
                                     h.cusip,
                                     h.report_period,
                                     h.manager_name,
                                     h.option_type,
                                     h.shares_or_prn_type,
                                     h.shares_or_prn_amount,
                                     h.value_usd,
                                     DENSE_RANK() OVER (PARTITION BY h.cik, h.report_period ORDER BY h.filing_date DESC, h.accession_number DESC) AS rn
                                  FROM t_sec_holding h
                                  WHERE h.report_period = p_report_period
                                  ),
              manager_holdings AS (SELECT
                                      h.cusip,
                                      h.report_period,
                                      MAX(h.manager_name) AS manager_name,
                                      SUM(h.value_usd)    AS value_usd
                                   FROM ranked_holdings h
                                   WHERE h.rn = 1
                                     AND h.option_type IS NULL
                                     AND (h.shares_or_prn_type IS NULL OR h.shares_or_prn_type = 'SH')
                                   GROUP BY h.cusip, h.report_period, h.cik
                                   ),
              ranked_holders AS (SELECT
                                    m.cusip,
                                    m.report_period,
                                    m.manager_name,
                                    m.value_usd,
                                    ROW_NUMBER() OVER (PARTITION BY m.cusip, m.report_period ORDER BY m.value_usd DESC) AS rn
                                 FROM manager_holdings m
                                 ),
              top_holders_agg AS (SELECT
                                     cusip,
                                     report_period,
                                     ARRAY_AGG(manager_name ORDER BY value_usd DESC) AS top_holders_names,
                                     SUM(value_usd)                                  AS top5_holders_value_usd
                                  FROM ranked_holders
                                  WHERE rn <= 5
                                  GROUP BY cusip, report_period
                                  ),
              current_holdings AS (SELECT
                                      h.cusip,
                                      h.report_period,
                                      SUM(CASE WHEN h.option_type IS NULL AND (h.shares_or_prn_type IS NULL OR h.shares_or_prn_type = 'SH') THEN h.value_usd ELSE 0 END)                           AS total_inst_value_usd,
                                      SUM(CASE WHEN h.option_type IS NULL AND (h.shares_or_prn_type IS NULL OR h.shares_or_prn_type = 'SH') THEN h.shares_or_prn_amount ELSE 0 END)                AS total_inst_shares,
                                      COUNT(DISTINCT CASE WHEN h.option_type IS NULL THEN h.cik END)                                                                                               AS total_holders_count,
                                      SUM(CASE WHEN UPPER(h.option_type) = 'PUT'  THEN h.value_usd ELSE 0 END)                                                                                     AS put_value_usd,
                                      SUM(CASE WHEN UPPER(h.option_type) = 'CALL' THEN h.value_usd ELSE 0 END)                                                                                     AS call_value_usd,
                                      SUM(CASE WHEN f.tier = 1 AND h.option_type IS NULL AND (h.shares_or_prn_type IS NULL OR h.shares_or_prn_type = 'SH') THEN h.value_usd ELSE 0 END)            AS tier1_value_usd,
                                      SUM(CASE WHEN f.tier = 1 AND h.option_type IS NULL AND (h.shares_or_prn_type IS NULL OR h.shares_or_prn_type = 'SH') THEN h.shares_or_prn_amount ELSE 0 END) AS tier1_shares,
                                      COUNT(DISTINCT CASE WHEN f.tier = 1 AND h.option_type IS NULL THEN h.cik END)                                                                                AS tier1_holders_count
                                   FROM ranked_holdings h
                                   LEFT JOIN t_sec_filer f ON f.cik = h.cik
                                   WHERE h.rn = 1
                                   GROUP BY h.cusip, h.report_period
                                   ),
               current_deltas AS (SELECT
                                     d.cusip,
                                     d.report_period,
                                     SUM(d.value_usd_change)                                                                                             AS net_value_change_usd,
                                     SUM(d.shares_change)                                                                                                AS net_shares_change,
                                     COUNT(DISTINCT CASE WHEN d.shares_change > 0 THEN d.cik END)                                                        AS funds_buying_count,
                                     COUNT(DISTINCT CASE WHEN d.shares_change < 0 OR d.is_closed_position THEN d.cik END)                                AS funds_selling_count,
                                     COUNT(DISTINCT CASE WHEN d.is_new_position THEN d.cik END)                                                          AS funds_new_count,
                                     COUNT(DISTINCT CASE WHEN d.is_closed_position THEN d.cik END)                                                       AS funds_closed_count,
                                     COUNT(DISTINCT CASE WHEN d.shares_change = 0 AND NOT d.is_new_position AND NOT d.is_closed_position THEN d.cik END) AS funds_unchanged_count,
                                     SUM(CASE WHEN f.tier = 1 THEN d.value_usd_change ELSE 0 END)                                                        AS tier1_net_value_change_usd,
                                     SUM(CASE WHEN f.tier = 1 THEN d.shares_change ELSE 0 END)                                                           AS tier1_net_shares_change,
                                     COUNT(DISTINCT CASE WHEN f.tier = 1 AND d.shares_change > 0 THEN d.cik END)                                         AS tier1_buyers_count,
                                     COUNT(DISTINCT CASE WHEN f.tier = 1 AND (d.shares_change < 0 OR d.is_closed_position) THEN d.cik END)               AS tier1_sellers_count,
                                     COUNT(DISTINCT CASE WHEN f.tier = 1 AND d.is_new_position THEN d.cik END)                                           AS tier1_new_count,
                                     COUNT(DISTINCT CASE WHEN f.tier = 1 AND d.is_closed_position THEN d.cik END)                                        AS tier1_closed_count,
                                     ARRAY_AGG(DISTINCT f.manager_name) FILTER (WHERE f.tier = 1 AND d.shares_change > 0)                                AS tier1_buyer_names,
                                     ARRAY_AGG(DISTINCT f.manager_name) FILTER (WHERE f.tier = 1 AND (d.shares_change < 0 OR d.is_closed_position))      AS tier1_seller_names
                                  FROM fn_sec_holding_delta(p_report_period) d
                                  LEFT JOIN t_sec_filer f ON f.cik = d.cik
                                  WHERE d.option_type IS NULL
                                  GROUP BY d.cusip, d.report_period
                                  ),
               aggregated_source AS (SELECT
                                        h.cusip,
                                        h.report_period,
                                        h.total_inst_value_usd,
                                        h.total_inst_shares,
                                        h.total_holders_count,
                                        top.top_holders_names,
                                        COALESCE(top.top5_holders_value_usd, 0)   AS top5_holders_value_usd,
                                        LEAST(ROUND(COALESCE(top.top5_holders_value_usd, 0) * 100.0 / NULLIF(h.total_inst_value_usd, 0), 2), 100.00) AS top5_holders_pct,
                                        h.put_value_usd,
                                        h.call_value_usd,
                                        ROUND(LEAST(h.put_value_usd * 1.0 / NULLIF(h.call_value_usd, 0), 999999999.99), 2) AS put_call_ratio,
                                        COALESCE(d.net_value_change_usd, 0)       AS net_value_change_usd,
                                        COALESCE(d.net_shares_change, 0)          AS net_shares_change,
                                        COALESCE(d.funds_buying_count, 0)         AS funds_buying_count,
                                        COALESCE(d.funds_selling_count, 0)        AS funds_selling_count,
                                        COALESCE(d.funds_new_count, 0)            AS funds_new_count,
                                        COALESCE(d.funds_closed_count, 0)         AS funds_closed_count,
                                        COALESCE(d.funds_unchanged_count, 0)      AS funds_unchanged_count,
                                        h.tier1_value_usd,
                                        h.tier1_shares,
                                        h.tier1_holders_count,
                                        COALESCE(d.tier1_net_value_change_usd, 0) AS tier1_net_value_change_usd,
                                        COALESCE(d.tier1_net_shares_change, 0)    AS tier1_net_shares_change,
                                        COALESCE(d.tier1_buyers_count, 0)         AS tier1_buyers_count,
                                        COALESCE(d.tier1_sellers_count, 0)        AS tier1_sellers_count,
                                        COALESCE(d.tier1_new_count, 0)            AS tier1_new_count,
                                        COALESCE(d.tier1_closed_count, 0)         AS tier1_closed_count,
                                        d.tier1_buyer_names,
                                        d.tier1_seller_names
                                     FROM current_holdings h
                                     LEFT JOIN top_holders_agg top ON top.cusip = h.cusip
                                                                  AND top.report_period = h.report_period
                                     LEFT JOIN current_deltas d ON d.cusip = h.cusip
                                                               AND d.report_period = h.report_period
                                     )
          SELECT *
          FROM aggregated_source
          ) AS source
  ON (target.cusip = source.cusip
      AND target.report_period = source.report_period)
  WHEN MATCHED THEN
    UPDATE SET
       total_inst_value_usd       = source.total_inst_value_usd,
       total_inst_shares          = source.total_inst_shares,
       total_holders_count        = source.total_holders_count,
       top_holders_names          = source.top_holders_names,
       top5_holders_value_usd     = source.top5_holders_value_usd,
       top5_holders_pct           = source.top5_holders_pct,
       put_value_usd              = source.put_value_usd,
       call_value_usd             = source.call_value_usd,
       put_call_ratio             = source.put_call_ratio,
       net_value_change_usd       = source.net_value_change_usd,
       net_shares_change          = source.net_shares_change,
       funds_buying_count         = source.funds_buying_count,
       funds_selling_count        = source.funds_selling_count,
       funds_new_count            = source.funds_new_count,
       funds_closed_count         = source.funds_closed_count,
       funds_unchanged_count      = source.funds_unchanged_count,
       tier1_value_usd            = source.tier1_value_usd,
       tier1_shares               = source.tier1_shares,
       tier1_holders_count        = source.tier1_holders_count,
       tier1_net_value_change_usd = source.tier1_net_value_change_usd,
       tier1_net_shares_change    = source.tier1_net_shares_change,
       tier1_buyers_count         = source.tier1_buyers_count,
       tier1_sellers_count        = source.tier1_sellers_count,
       tier1_new_count            = source.tier1_new_count,
       tier1_closed_count         = source.tier1_closed_count,
       tier1_buyer_names          = source.tier1_buyer_names,
       tier1_seller_names         = source.tier1_seller_names,
       updated_at                 = CURRENT_TIMESTAMP
  WHEN NOT MATCHED THEN
    INSERT
      (cusip, report_period,
       total_inst_value_usd, total_inst_shares, total_holders_count,
       top_holders_names, top5_holders_value_usd, top5_holders_pct,
       put_value_usd, call_value_usd, put_call_ratio,
       net_value_change_usd, net_shares_change,
       funds_buying_count, funds_selling_count, funds_new_count, funds_closed_count, funds_unchanged_count,
       tier1_value_usd, tier1_shares, tier1_holders_count,
       tier1_net_value_change_usd, tier1_net_shares_change,
       tier1_buyers_count, tier1_sellers_count, tier1_new_count, tier1_closed_count,
       tier1_buyer_names, tier1_seller_names,
       created_at, updated_at)
    VALUES
      (source.cusip, source.report_period,
       source.total_inst_value_usd, source.total_inst_shares, source.total_holders_count,
       source.top_holders_names, source.top5_holders_value_usd, source.top5_holders_pct,
       source.put_value_usd, source.call_value_usd, source.put_call_ratio,
       source.net_value_change_usd, source.net_shares_change,
       source.funds_buying_count, source.funds_selling_count, source.funds_new_count, source.funds_closed_count, source.funds_unchanged_count,
       source.tier1_value_usd, source.tier1_shares, source.tier1_holders_count,
       source.tier1_net_value_change_usd, source.tier1_net_shares_change,
       source.tier1_buyers_count, source.tier1_sellers_count, source.tier1_new_count, source.tier1_closed_count,
       source.tier1_buyer_names, source.tier1_seller_names,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  WHEN NOT MATCHED BY SOURCE AND target.report_period = p_report_period THEN DELETE;
END;
$$;
