-- =================================================================
-- REPEATABLE: R__08_fn_sec_holding_delta.sql
-- Description: Core 13F holding delta calculation function.
--              Calculates quarter-over-quarter position changes
--              directly from t_sec_holding with optional cusip and cik filters.
-- =================================================================

CREATE OR REPLACE FUNCTION public.fn_sec_holding_delta(
    p_current_period  DATE,
    p_previous_period DATE    DEFAULT NULL,
    p_cusip           VARCHAR DEFAULT NULL,
    p_cik             VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    cik                VARCHAR(10),
    report_period      DATE,
    cusip              VARCHAR(9),
    issuer_name        VARCHAR(255),
    option_type        VARCHAR(4),
    value_usd_current  BIGINT,
    value_usd_previous BIGINT,
    value_usd_change   BIGINT,
    shares_current     BIGINT,
    shares_previous    BIGINT,
    shares_change      BIGINT,
    is_new_position    BOOLEAN,
    is_closed_position BOOLEAN
)
LANGUAGE sql STABLE AS $$
  WITH target_periods AS (SELECT
                             p_current_period AS cur_period,
                             COALESCE(p_previous_period, (date_trunc('quarter', p_current_period) - INTERVAL '1 day')::DATE) AS prv_period
                          ),
       ranked_holdings AS (SELECT
                              h.cik,
                              h.report_period,
                              h.cusip,
                              h.issuer_name,
                              h.option_type,
                              h.value_usd,
                              h.shares_or_prn_amount,
                              DENSE_RANK() OVER (PARTITION BY h.cik, h.report_period ORDER BY h.filing_date DESC, h.accession_number DESC) AS rn
                           FROM t_sec_holding h
                           JOIN target_periods tp ON h.report_period IN (tp.cur_period, tp.prv_period)
                           WHERE (p_cusip IS NULL OR h.cusip = p_cusip)
                             AND (p_cik   IS NULL OR h.cik   = p_cik)
                           ),
       holdings_summary AS (SELECT
                               r.cik,
                               r.report_period,
                               r.cusip,
                               MAX(r.issuer_name)          AS issuer_name,
                               r.option_type,
                               SUM(r.value_usd)            AS value_usd,
                               SUM(r.shares_or_prn_amount) AS shares_amount
                            FROM ranked_holdings r
                            WHERE r.rn = 1
                            GROUP BY r.cik, r.report_period, r.cusip, r.option_type
                            ),
       current_holdings AS (SELECT
                               hs.cik,
                               hs.cusip,
                               hs.issuer_name,
                               hs.option_type,
                               hs.value_usd,
                               hs.shares_amount
                            FROM holdings_summary hs
                            JOIN target_periods tp ON hs.report_period = tp.cur_period
                            ),
       previous_holdings AS (SELECT
                                hs.cik,
                                hs.cusip,
                                hs.issuer_name,
                                hs.option_type,
                                hs.value_usd,
                                hs.shares_amount
                             FROM holdings_summary hs
                             JOIN target_periods tp ON hs.report_period = tp.prv_period
                             )
  SELECT
      COALESCE(cur.cik, prv.cik)                                      AS cik,
      tp.cur_period                                                   AS report_period,
      COALESCE(cur.cusip, prv.cusip)                                  AS cusip,
      COALESCE(cur.issuer_name, prv.issuer_name)                      AS issuer_name,
      COALESCE(cur.option_type, prv.option_type)                      AS option_type,
      COALESCE(cur.value_usd, 0)                                      AS value_usd_current,
      prv.value_usd                                                   AS value_usd_previous,
      COALESCE(cur.value_usd, 0) - COALESCE(prv.value_usd, 0)         AS value_usd_change,
      COALESCE(cur.shares_amount, 0)                                  AS shares_current,
      prv.shares_amount                                               AS shares_previous,
      COALESCE(cur.shares_amount, 0) - COALESCE(prv.shares_amount, 0) AS shares_change,
      (prv.cik IS NULL)                                               AS is_new_position,
      (cur.cik IS NULL)                                               AS is_closed_position
  FROM current_holdings cur
  FULL OUTER JOIN previous_holdings prv ON prv.cik = cur.cik
                                       AND prv.cusip = cur.cusip
                                       AND (prv.option_type IS NOT DISTINCT FROM cur.option_type)
  CROSS JOIN target_periods tp;
$$;
