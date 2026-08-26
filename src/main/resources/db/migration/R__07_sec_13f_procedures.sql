-- ============================================================
-- R__07 — SEC 13F stored procedures (repeatable)
-- ============================================================
-- Repeatable scripts are re-run whenever their checksum changes,
-- so procedures here can be modified freely without a new Vxx migration.
-- ============================================================

-- ── sp_compute_sec_holding_delta ─────────────────────────────
-- Recomputes quarter-over-quarter position changes for a given period.
--
-- Strategy:
--   1. TRUNCATE only the target quarter's partition (no full-table lock).
--   2. INSERT all current-quarter positions joined to previous-quarter
--      (LEFT JOIN) to derive new/changed/closed positions.
--
-- Called once per quarter by SecHoldingFileLoadServiceImpl.postBatchComplete()
-- immediately after all Parquet chunks for p_current_period have loaded.
-- ─────────────────────────────────────────────────────────────
CREATE OR REPLACE PROCEDURE sp_compute_sec_holding_delta(
    p_current_period  DATE,
    p_previous_period DATE
)
LANGUAGE plpgsql AS $$
DECLARE
    v_partition TEXT;
BEGIN
    -- Truncate only the current quarter's partition to avoid a full-table
    -- ACCESS EXCLUSIVE lock on t_sec_holding_delta.
    v_partition := quarterly_partition_name('t_sec_holding_delta', p_current_period);
    IF to_regclass(v_partition) IS NOT NULL THEN
        EXECUTE format('TRUNCATE %I', v_partition);
    END IF;

    INSERT INTO t_sec_holding_delta (
        cik, period_of_report, cusip, issuer_name,
        value_usd_current,  value_usd_previous,  value_usd_change,
        shares_current,     shares_previous,      shares_change,
        is_new_position,    is_closed_position
    )

    -- ① Current positions (new + changed + unchanged)
    SELECT
        cur.cik,
        cur.period_of_report,
        cur.cusip,
        cur.issuer_name,
        cur.value_usd                                                    AS value_usd_current,
        prv.value_usd                                                    AS value_usd_previous,
        cur.value_usd            - COALESCE(prv.value_usd, 0)           AS value_usd_change,
        cur.shares_or_prn_amount                                         AS shares_current,
        prv.shares_or_prn_amount                                         AS shares_previous,
        cur.shares_or_prn_amount - COALESCE(prv.shares_or_prn_amount, 0) AS shares_change,
        prv.cik IS NULL                                                  AS is_new_position,
        FALSE                                                            AS is_closed_position
    FROM t_sec_holding cur
    LEFT JOIN t_sec_holding prv
           ON prv.cik                       = cur.cik
          AND prv.cusip                      = cur.cusip
          AND COALESCE(prv.option_type, '') = COALESCE(cur.option_type, '')
          AND prv.period_of_report           = p_previous_period
    WHERE cur.period_of_report = p_current_period

    UNION ALL

    -- ② Closed positions: held last quarter, absent this quarter
    SELECT
        prv.cik,
        p_current_period,
        prv.cusip,
        prv.issuer_name,
        0                        AS value_usd_current,
        prv.value_usd            AS value_usd_previous,
        -prv.value_usd           AS value_usd_change,
        0                        AS shares_current,
        prv.shares_or_prn_amount AS shares_previous,
        -prv.shares_or_prn_amount AS shares_change,
        FALSE                    AS is_new_position,
        TRUE                     AS is_closed_position
    FROM t_sec_holding prv
    WHERE prv.period_of_report = p_previous_period
      AND NOT EXISTS (
          SELECT 1 FROM t_sec_holding cur
           WHERE cur.cik                       = prv.cik
             AND cur.cusip                      = prv.cusip
             AND COALESCE(cur.option_type, '') = COALESCE(prv.option_type, '')
             AND cur.period_of_report           = p_current_period
      )

    ON CONFLICT (cik, cusip, period_of_report) DO NOTHING;
END;
$$;
